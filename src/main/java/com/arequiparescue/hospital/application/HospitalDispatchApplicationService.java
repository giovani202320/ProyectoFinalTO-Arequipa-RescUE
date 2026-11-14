package com.arequiparescue.hospital.application;

import com.arequiparescue.ambulance.domain.AmbulanceId;
import com.arequiparescue.ambulance.domain.port.AmbulanceRepository;
import com.arequiparescue.dispatch.domain.port.DispatchTraceabilityRepository;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.emergency.domain.EmergencyState;
import com.arequiparescue.emergency.domain.port.EmergencyRepository;
import com.arequiparescue.hospital.domain.EligibleHospitalCandidate;
import com.arequiparescue.hospital.domain.Hospital;
import com.arequiparescue.hospital.domain.HospitalDispatchOutcome;
import com.arequiparescue.hospital.domain.HospitalEligibilityPolicy;
import com.arequiparescue.hospital.domain.WaitingHospitalQueuePolicy;
import com.arequiparescue.hospital.domain.port.HospitalRepository;
import com.arequiparescue.routing.domain.RoadGraph;
import com.arequiparescue.routing.domain.port.RoadGraphRepository;
import com.arequiparescue.shared.domain.LogicalClock;
import com.arequiparescue.shared.domain.LogicalTime;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Coordinador de aplicación para llegada al incidente (HU-09), reserva hospitalaria concurrente
 * con reintentos acotados 1+2 (DEC-018, ADR-0002) y reevaluación FIFO de la cola
 * {@code WAITING_HOSPITAL} sin pérdida de posición (HU-10).
 */
@Service
public class HospitalDispatchApplicationService {

    public static final int MAX_HOSPITAL_ATTEMPTS = 3;

    private static final String OP_HOSPITAL_RESERVATION = "HOSPITAL_RESERVATION";
    private static final String OP_WAITING_HOSPITAL_QUEUE = "WAITING_HOSPITAL_QUEUE";

    private final EmergencyRepository emergencyRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final HospitalRepository hospitalRepository;
    private final RoadGraphRepository roadGraphRepository;
    private final DispatchTraceabilityRepository traceabilityRepository;
    private final HospitalReservationAttemptExecutor attemptExecutor;
    private final LogicalClock logicalClock;
    private final HospitalEligibilityPolicy eligibilityPolicy;
    private final WaitingHospitalQueuePolicy waitingHospitalQueuePolicy;

    public HospitalDispatchApplicationService(
            EmergencyRepository emergencyRepository,
            AmbulanceRepository ambulanceRepository,
            HospitalRepository hospitalRepository,
            RoadGraphRepository roadGraphRepository,
            DispatchTraceabilityRepository traceabilityRepository,
            HospitalReservationAttemptExecutor attemptExecutor,
            LogicalClock logicalClock
    ) {
        this.emergencyRepository = Objects.requireNonNull(emergencyRepository);
        this.ambulanceRepository = Objects.requireNonNull(ambulanceRepository);
        this.hospitalRepository = Objects.requireNonNull(hospitalRepository);
        this.roadGraphRepository = Objects.requireNonNull(roadGraphRepository);
        this.traceabilityRepository = Objects.requireNonNull(traceabilityRepository);
        this.attemptExecutor = Objects.requireNonNull(attemptExecutor);
        this.logicalClock = Objects.requireNonNull(logicalClock);
        this.eligibilityPolicy = new HospitalEligibilityPolicy();
        this.waitingHospitalQueuePolicy = new WaitingHospitalQueuePolicy();
    }

    /**
     * HU-09: Confirma la llegada al incidente (ASSIGNED -> AT_INCIDENT) y ejecuta la selección
     * y reserva hospitalaria concurrente (1 intento inicial + hasta 2 reintentos).
     */
    public HospitalReservationResult confirmArrivalAndReserveHospital(String rawEmergencyId) {
        EmergencyId emergencyId = parseEmergencyId(rawEmergencyId);
        LogicalTime now = logicalClock.currentTime();
        String operationId = "HOSP-ARR-" + UUID.randomUUID();

        HospitalReservationAttemptExecutor.ArrivalOutcome arrival =
                attemptExecutor.confirmArrivalAtIncident(emergencyId, now);

        if (arrival.duplicateOrInvalid()) {
            return new HospitalReservationResult(
                    operationId,
                    HospitalDispatchOutcome.DUPLICATE_OR_INVALID_STATE,
                    arrival.emergency(),
                    arrival.ambulance(),
                    null,
                    0,
                    0,
                    "DUPLICATE_OR_INVALID_STATE",
                    arrival.message()
            );
        }

        return executeReservationRetryLoop(operationId, OP_HOSPITAL_RESERVATION, emergencyId, now);
    }

    /**
     * HU-10: Reevalúa automáticamente la cola {@code WAITING_HOSPITAL} en estricto orden FIFO
     * sin hacer perder posición a emergencias cuyo hospital liberado sea inalcanzable (CA-HOS-09).
     */
    public WaitingHospitalQueueEvaluationResult evaluateWaitingHospitalQueue() {
        LogicalTime now = logicalClock.currentTime();
        List<Emergency> fifoQueue = listWaitingHospitalQueue();
        List<HospitalReservationResult> results = new ArrayList<>();
        int reservedCount = 0;

        for (Emergency waitingEmergency : fifoQueue) {
            Emergency fresh = emergencyRepository.findById(waitingEmergency.id()).orElse(null);
            if (fresh == null || fresh.state() != EmergencyState.WAITING_HOSPITAL) {
                continue;
            }
            List<EligibleHospitalCandidate> eligible = eligibilityPolicy.evaluateEligibleHospitals(
                    fresh,
                    hospitalRepository.findAllOrderById(),
                    roadGraphRepository.loadGraph()
            );
            if (eligible.isEmpty()) {
                // CA-HOS-09: conserva intacta su marca hospitalQueueEnteredAt y posición FIFO; sigue con la siguiente
                continue;
            }

            String operationId = "HOSP-Q-" + UUID.randomUUID();
            HospitalReservationResult res = executeReservationRetryLoop(
                    operationId, OP_WAITING_HOSPITAL_QUEUE, fresh.id(), now);
            results.add(res);
            if (res.outcome() == HospitalDispatchOutcome.IN_TRANSPORT) {
                reservedCount++;
            }
        }

        int remaining = listWaitingHospitalQueue().size();
        String summary = fifoQueue.isEmpty()
                ? "No hay emergencias en cola WAITING_HOSPITAL."
                : "Cola WAITING_HOSPITAL reevaluada (" + fifoQueue.size() + " en espera): "
                        + reservedCount + " reserva(s) confirmada(s) en orden FIFO; permanecen "
                        + remaining + " en espera.";
        return new WaitingHospitalQueueEvaluationResult(
                fifoQueue.size(),
                reservedCount,
                remaining,
                List.copyOf(results),
                summary
        );
    }

    @Transactional(readOnly = true)
    public List<Hospital> listHospitalsOrderById() {
        return hospitalRepository.findAllOrderById();
    }

    @Transactional(readOnly = true)
    public List<Emergency> listAssignedEmergencies() {
        return emergencyRepository.findByStateOrderByRegisteredAtAndId(EmergencyState.ASSIGNED);
    }

    @Transactional(readOnly = true)
    public List<Emergency> listWaitingHospitalQueue() {
        return waitingHospitalQueuePolicy.sortFifo(
                emergencyRepository.findWaitingHospitalOrderByHospitalQueueEnteredAtAndId()
        );
    }

    @Transactional(readOnly = true)
    public List<DispatchTraceabilityRepository.ExhaustedDispatchNotice> listRecentHospitalExhaustedNotices() {
        return traceabilityRepository.findRecentHospitalExhaustedNotices();
    }

    private HospitalReservationResult executeReservationRetryLoop(
            String operationId,
            String operationType,
            EmergencyId emergencyId,
            LogicalTime now
    ) {
        int executedAttempts = 0;

        for (int attempt = 1; attempt <= MAX_HOSPITAL_ATTEMPTS; attempt++) {
            Emergency freshEmergency = emergencyRepository.findById(emergencyId)
                    .orElseThrow(() -> new HospitalCatalogValidationException(
                            "No existe ninguna emergencia con identificador " + emergencyId.value() + "."));

            if (freshEmergency.state() != EmergencyState.AT_INCIDENT
                    && freshEmergency.state() != EmergencyState.WAITING_HOSPITAL) {
                return new HospitalReservationResult(
                        operationId,
                        HospitalDispatchOutcome.DUPLICATE_OR_INVALID_STATE,
                        freshEmergency,
                        null,
                        null,
                        executedAttempts,
                        Math.max(0, executedAttempts - 1),
                        "DUPLICATE_OR_INVALID_STATE",
                        "La emergencia " + emergencyId.value() + " se encuentra en estado " + freshEmergency.state() + "."
                );
            }

            List<Hospital> hospitals = hospitalRepository.findAllOrderById();
            RoadGraph graph = roadGraphRepository.loadGraph();
            List<EligibleHospitalCandidate> candidates =
                    eligibilityPolicy.evaluateEligibleHospitals(freshEmergency, hospitals, graph);

            if (candidates.isEmpty()) {
                Emergency waiting = attemptExecutor.moveEmergencyToWaitingHospital(
                        emergencyId, now, "NO_ELIGIBLE_HOSPITAL");
                String reason = attempt == 1
                        ? "Sin hospitales habilitados, alcanzables y con plazas disponibles (D >= 1); "
                                + emergencyId.value() + " pasa a WAITING_HOSPITAL reteniendo ambulancia en AT_INCIDENT (0 reintentos)."
                        : "Tras conflicto concurrente ya no quedan hospitales elegibles; finalización anticipada en WAITING_HOSPITAL.";
                traceabilityRepository.recordOperationResult(
                        operationId, operationType, emergencyId.value(), null,
                        executedAttempts, "MOVED_TO_WAITING_HOSPITAL", reason, now.ticks());
                return new HospitalReservationResult(
                        operationId,
                        HospitalDispatchOutcome.MOVED_TO_WAITING_HOSPITAL,
                        waiting,
                        waiting.assignedAmbulanceId() != null
                                ? ambulanceRepository.findById(AmbulanceId.of(waiting.assignedAmbulanceId())).orElse(null)
                                : null,
                        null,
                        executedAttempts,
                        Math.max(0, executedAttempts - 1),
                        "NO_ELIGIBLE_HOSPITAL",
                        reason
                );
            }

            EligibleHospitalCandidate chosen = candidates.get(0);
            executedAttempts = attempt;

            HospitalReservationAttemptExecutor.ReservationAttemptOutcome attemptOutcome;
            try {
                attemptOutcome = attemptExecutor.executeHospitalReservationAttempt(
                        emergencyId, chosen.hospital().id(), now);
            } catch (PessimisticLockingFailureException ex) {
                attemptOutcome = new HospitalReservationAttemptExecutor.ReservationAttemptOutcome(
                        false,
                        true,
                        freshEmergency,
                        null,
                        chosen.hospital(),
                        "Conflicto concurrente de bloqueo en intento " + attempt + " sobre " + chosen.hospital().id().value() + "."
                );
            } catch (RuntimeException ex) {
                recordTechnicalErrorBestEffort(operationId, operationType, emergencyId.value(),
                        chosen.hospital().id().value(), attempt, ex.getMessage(), now.ticks());
                throw ex;
            }

            if (attemptOutcome.succeeded()) {
                traceabilityRepository.recordAttempt(
                        operationId, operationType, emergencyId.value(), chosen.hospital().id().value(),
                        attempt, "RESERVED", attemptOutcome.reason(), now.ticks());
                traceabilityRepository.recordOperationResult(
                        operationId, operationType, emergencyId.value(), chosen.hospital().id().value(),
                        attempt, "IN_TRANSPORT", attemptOutcome.reason(), now.ticks());
                return new HospitalReservationResult(
                        operationId,
                        HospitalDispatchOutcome.IN_TRANSPORT,
                        attemptOutcome.emergency(),
                        attemptOutcome.ambulance(),
                        attemptOutcome.hospital(),
                        attempt,
                        attempt - 1,
                        "IN_TRANSPORT",
                        attemptOutcome.reason()
                );
            }

            if (!attemptOutcome.conflict()) {
                return new HospitalReservationResult(
                        operationId,
                        HospitalDispatchOutcome.DUPLICATE_OR_INVALID_STATE,
                        attemptOutcome.emergency(),
                        attemptOutcome.ambulance(),
                        null,
                        executedAttempts,
                        Math.max(0, executedAttempts - 1),
                        "DUPLICATE_OR_INVALID_STATE",
                        attemptOutcome.reason()
                );
            }

            traceabilityRepository.recordAttempt(
                    operationId, operationType, emergencyId.value(), chosen.hospital().id().value(),
                    attempt, "HOSPITAL_CAPACITY_CONFLICT", attemptOutcome.reason(), now.ticks());
        }

        String exhaustedMessage = "Agotamiento de reintentos de reserva hospitalaria (HOSPITAL_RESERVATION_RETRY_EXHAUSTED) para "
                + emergencyId.value() + " tras 3 intentos (2 reintentos consumidos). Permanece en WAITING_HOSPITAL reteniendo ambulancia en AT_INCIDENT.";
        Emergency waiting = attemptExecutor.moveEmergencyToWaitingHospital(
                emergencyId, now, "HOSPITAL_RESERVATION_RETRY_EXHAUSTED");
        traceabilityRepository.recordOperationResult(
                operationId, operationType, emergencyId.value(), null,
                MAX_HOSPITAL_ATTEMPTS, "HOSPITAL_RESERVATION_RETRY_EXHAUSTED", exhaustedMessage, now.ticks());
        return new HospitalReservationResult(
                operationId,
                HospitalDispatchOutcome.HOSPITAL_RESERVATION_RETRY_EXHAUSTED,
                waiting,
                waiting.assignedAmbulanceId() != null
                        ? ambulanceRepository.findById(AmbulanceId.of(waiting.assignedAmbulanceId())).orElse(null)
                        : null,
                null,
                MAX_HOSPITAL_ATTEMPTS,
                MAX_HOSPITAL_ATTEMPTS - 1,
                "HOSPITAL_RESERVATION_RETRY_EXHAUSTED",
                exhaustedMessage
        );
    }

    private void recordTechnicalErrorBestEffort(
            String operationId,
            String operationType,
            String emergencyId,
            String hospitalId,
            int attemptNumber,
            String detailMessage,
            long logicalTicks
    ) {
        String safe = detailMessage == null || detailMessage.isBlank()
                ? "Error técnico no reintentable en reserva hospitalaria."
                : detailMessage;
        try {
            traceabilityRepository.recordAttempt(
                    operationId, operationType, emergencyId, hospitalId,
                    attemptNumber, "TECHNICAL_ERROR", safe, logicalTicks);
            traceabilityRepository.recordOperationResult(
                    operationId, operationType, emergencyId, null,
                    attemptNumber, "TECHNICAL_ERROR", safe, logicalTicks);
        } catch (RuntimeException ignored) {
            // No ocultar el error original
        }
    }

    private static EmergencyId parseEmergencyId(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new HospitalCatalogValidationException("El identificador de la emergencia es obligatorio.");
        }
        try {
            return EmergencyId.of(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new HospitalCatalogValidationException(ex.getMessage());
        }
    }
}

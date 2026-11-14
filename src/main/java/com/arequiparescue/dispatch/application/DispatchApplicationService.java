package com.arequiparescue.dispatch.application;

import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.ambulance.domain.AmbulanceId;
import com.arequiparescue.ambulance.domain.port.AmbulanceRepository;
import com.arequiparescue.dispatch.domain.AmbulanceEligibilityPolicy;
import com.arequiparescue.dispatch.domain.DispatchOperationOutcome;
import com.arequiparescue.dispatch.domain.EligibleAmbulanceCandidate;
import com.arequiparescue.dispatch.domain.port.DispatchTraceabilityRepository;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.emergency.domain.EmergencyState;
import com.arequiparescue.emergency.domain.port.EmergencyRepository;
import com.arequiparescue.routing.domain.RoadGraph;
import com.arequiparescue.routing.domain.port.RoadGraphRepository;
import com.arequiparescue.shared.domain.LogicalClock;
import com.arequiparescue.shared.domain.LogicalTime;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Coordinador de aplicación para evaluación de ambulancias elegibles (HU-05) y asignación
 * supervisada con revalidación concurrente sin reintentos automáticos (HU-06, ADR-0002).
 */
@Service
public class DispatchApplicationService {

    private static final String OP_SUPERVISED = "SUPERVISED_DISPATCH";

    private final EmergencyRepository emergencyRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final RoadGraphRepository roadGraphRepository;
    private final DispatchTraceabilityRepository traceabilityRepository;
    private final SupervisedDispatchAttemptExecutor attemptExecutor;
    private final LogicalClock logicalClock;
    private final AmbulanceEligibilityPolicy eligibilityPolicy;

    public DispatchApplicationService(
            EmergencyRepository emergencyRepository,
            AmbulanceRepository ambulanceRepository,
            RoadGraphRepository roadGraphRepository,
            DispatchTraceabilityRepository traceabilityRepository,
            SupervisedDispatchAttemptExecutor attemptExecutor,
            LogicalClock logicalClock
    ) {
        this.emergencyRepository = Objects.requireNonNull(emergencyRepository);
        this.ambulanceRepository = Objects.requireNonNull(ambulanceRepository);
        this.roadGraphRepository = Objects.requireNonNull(roadGraphRepository);
        this.traceabilityRepository = Objects.requireNonNull(traceabilityRepository);
        this.attemptExecutor = Objects.requireNonNull(attemptExecutor);
        this.logicalClock = Objects.requireNonNull(logicalClock);
        this.eligibilityPolicy = new AmbulanceEligibilityPolicy();
    }

    public record EligibleEvaluation(
            Emergency emergency,
            List<EligibleAmbulanceCandidate> candidates
    ) {
    }

    @Transactional(readOnly = true)
    public EligibleEvaluation evaluateEligibleAmbulances(String rawEmergencyId) {
        EmergencyId emergencyId = parseEmergencyId(rawEmergencyId);
        Emergency emergency = emergencyRepository.findById(emergencyId)
                .orElseThrow(() -> new DispatchValidationException(
                        "No existe ninguna emergencia con identificador " + emergencyId.value() + "."
                ));
        if (emergency.state() != EmergencyState.REGISTERED
                && emergency.state() != EmergencyState.WAITING_AMBULANCE) {
            throw new DispatchValidationException(
                    "Solo se pueden consultar ambulancias elegibles para emergencias en REGISTERED o WAITING_AMBULANCE. Estado actual: "
                            + emergency.state());
        }
        List<Ambulance> catalog = ambulanceRepository.findAllOrderById();
        RoadGraph graph = roadGraphRepository.loadGraph();
        List<EligibleAmbulanceCandidate> candidates =
                eligibilityPolicy.evaluateEligibleCandidates(emergency, catalog, graph);
        return new EligibleEvaluation(emergency, candidates);
    }

    /**
     * Ejecuta la asignación supervisada (HU-06) en intento único (maxAttempts = 1, RN-CON-005).
     * Ante conflicto concurrente recarga las alternativas elegibles o transiciona a WAITING_AMBULANCE.
     */
    public SupervisedDispatchResult assignSupervised(String rawEmergencyId, String rawAmbulanceId) {
        EmergencyId emergencyId = parseEmergencyId(rawEmergencyId);
        AmbulanceId ambulanceId = parseAmbulanceId(rawAmbulanceId);
        String operationId = "SUP-" + UUID.randomUUID();
        LogicalTime now = logicalClock.currentTime();

        SupervisedDispatchAttemptExecutor.AttemptOutcome attemptOutcome;
        try {
            attemptOutcome = attemptExecutor.executeSupervisedAssignmentAttempt(emergencyId, ambulanceId, now);
        } catch (PessimisticLockingFailureException | DataIntegrityViolationException ex) {
            Emergency current = emergencyRepository.findById(emergencyId)
                    .orElseThrow(() -> new DispatchValidationException(
                            "No existe ninguna emergencia con identificador " + emergencyId.value() + "."));
            attemptOutcome = new SupervisedDispatchAttemptExecutor.AttemptOutcome(
                    false,
                    true,
                    current,
                    null,
                    "Conflicto concurrente de bloqueo al intentar asignar la ambulancia " + ambulanceId.value() + "."
            );
        }

        if (attemptOutcome.succeeded()) {
            traceabilityRepository.recordAttempt(
                    operationId,
                    OP_SUPERVISED,
                    emergencyId.value(),
                    ambulanceId.value(),
                    1,
                    "ASSIGNED",
                    attemptOutcome.reason(),
                    now.ticks()
            );
            traceabilityRepository.recordOperationResult(
                    operationId,
                    OP_SUPERVISED,
                    emergencyId.value(),
                    ambulanceId.value(),
                    1,
                    "ASSIGNED",
                    attemptOutcome.reason(),
                    now.ticks()
            );
            return new SupervisedDispatchResult(
                    operationId,
                    DispatchOperationOutcome.ASSIGNED,
                    attemptOutcome.emergency(),
                    attemptOutcome.ambulance(),
                    List.of(),
                    attemptOutcome.reason()
            );
        }

        // Conflicto en modalidad supervisada: no reintentar automáticamente (RN-CON-002, RN-CON-005)
        traceabilityRepository.recordAttempt(
                operationId,
                OP_SUPERVISED,
                emergencyId.value(),
                ambulanceId.value(),
                1,
                "AMBULANCE_UNAVAILABLE_CONFLICT",
                attemptOutcome.reason(),
                now.ticks()
        );

        Emergency freshEmergency = emergencyRepository.findById(emergencyId)
                .orElse(attemptOutcome.emergency());
        List<EligibleAmbulanceCandidate> refreshed = List.of();
        if (freshEmergency.state() == EmergencyState.REGISTERED
                || freshEmergency.state() == EmergencyState.WAITING_AMBULANCE) {
            refreshed = eligibilityPolicy.evaluateEligibleCandidates(
                    freshEmergency,
                    ambulanceRepository.findAllOrderById(),
                    roadGraphRepository.loadGraph()
            );
        }

        if (!refreshed.isEmpty()) {
            String message = attemptOutcome.reason()
                    + " Seleccione manualmente otra ambulancia de la lista actualizada.";
            traceabilityRepository.recordOperationResult(
                    operationId,
                    OP_SUPERVISED,
                    emergencyId.value(),
                    null,
                    1,
                    "CONFLICT_RELOAD_REQUIRED",
                    message,
                    now.ticks()
            );
            return new SupervisedDispatchResult(
                    operationId,
                    DispatchOperationOutcome.CONFLICT_RELOAD_REQUIRED,
                    freshEmergency,
                    null,
                    refreshed,
                    message
            );
        }

        String waitingReason = attemptOutcome.reason()
                + " No quedan ambulancias elegibles disponibles; la emergencia pasa/permanece en WAITING_AMBULANCE.";
        Emergency waitingEmergency = attemptExecutor.moveEmergencyToWaitingAmbulance(
                emergencyId,
                now,
                "SUPERVISED_CONFLICT_NO_ELIGIBLE"
        );
        traceabilityRepository.recordOperationResult(
                operationId,
                OP_SUPERVISED,
                emergencyId.value(),
                null,
                1,
                "MOVED_TO_WAITING_AMBULANCE",
                waitingReason,
                now.ticks()
        );
        return new SupervisedDispatchResult(
                operationId,
                DispatchOperationOutcome.MOVED_TO_WAITING_AMBULANCE,
                waitingEmergency,
                null,
                List.of(),
                waitingReason
        );
    }

    /**
     * Registra la permanencia o transición a WAITING_AMBULANCE (T-EME-02) cuando al evaluar no
     * existen ambulancias elegibles disponibles (RN-ASI-004, HU-06 CA-3).
     */
    public SupervisedDispatchResult moveToWaitingWhenNoEligible(String rawEmergencyId) {
        EmergencyId emergencyId = parseEmergencyId(rawEmergencyId);
        EligibleEvaluation evaluation = evaluateEligibleAmbulances(emergencyId.value());
        if (!evaluation.candidates().isEmpty()) {
            return new SupervisedDispatchResult(
                    "EVAL-" + UUID.randomUUID(),
                    DispatchOperationOutcome.CONFLICT_RELOAD_REQUIRED,
                    evaluation.emergency(),
                    null,
                    evaluation.candidates(),
                    "Existen ambulancias elegibles disponibles para selección supervisada."
            );
        }

        String operationId = "WAIT-" + UUID.randomUUID();
        LogicalTime now = logicalClock.currentTime();
        String reason = "Sin ambulancias elegibles disponibles/alcanzables; la emergencia queda en WAITING_AMBULANCE (RN-ASI-004).";
        Emergency waiting = attemptExecutor.moveEmergencyToWaitingAmbulance(
                emergencyId,
                now,
                "NO_ELIGIBLE_AMBULANCE"
        );
        traceabilityRepository.recordOperationResult(
                operationId,
                OP_SUPERVISED,
                emergencyId.value(),
                null,
                0,
                "MOVED_TO_WAITING_AMBULANCE",
                reason,
                now.ticks()
        );
        return new SupervisedDispatchResult(
                operationId,
                DispatchOperationOutcome.MOVED_TO_WAITING_AMBULANCE,
                waiting,
                null,
                List.of(),
                reason
        );
    }

    private static EmergencyId parseEmergencyId(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DispatchValidationException("El identificador de la emergencia es obligatorio.");
        }
        try {
            return EmergencyId.of(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new DispatchValidationException(ex.getMessage());
        }
    }

    private static AmbulanceId parseAmbulanceId(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DispatchValidationException("Debe seleccionar una ambulancia elegible para asignar.");
        }
        try {
            return AmbulanceId.of(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new DispatchValidationException(ex.getMessage());
        }
    }
}

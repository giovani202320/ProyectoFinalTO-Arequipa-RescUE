package com.arequiparescue.hospital.application;

import com.arequiparescue.dispatch.application.DispatchApplicationService;
import com.arequiparescue.dispatch.application.WaitingQueueEvaluationResult;
import com.arequiparescue.dispatch.domain.port.DispatchTraceabilityRepository;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.emergency.domain.EmergencyState;
import com.arequiparescue.emergency.domain.port.EmergencyRepository;
import com.arequiparescue.hospital.domain.HospitalDeliveryOutcome;
import com.arequiparescue.hospital.domain.HospitalDischargeOutcome;
import com.arequiparescue.hospital.domain.HospitalReservation;
import com.arequiparescue.hospital.domain.port.HospitalReservationRepository;
import com.arequiparescue.shared.domain.LogicalClock;
import com.arequiparescue.shared.domain.LogicalTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Coordinador de aplicación para la entrega hospitalaria simulada (HU-11) y el alta hospitalaria
 * simulada idempotente (HU-12), asegurando la separación entre la transacción atómica de liberación
 * y la reevaluación posterior de las colas {@code WAITING_AMBULANCE} y {@code WAITING_HOSPITAL} (ADR-0002).
 */
@Service
public class HospitalDeliveryAndDischargeApplicationService {

    private static final String OP_HOSPITAL_DELIVERY = "HOSPITAL_DELIVERY";
    private static final String OP_HOSPITAL_DISCHARGE = "HOSPITAL_DISCHARGE";

    private final EmergencyRepository emergencyRepository;
    private final HospitalReservationRepository hospitalReservationRepository;
    private final DispatchTraceabilityRepository traceabilityRepository;
    private final HospitalDeliveryAndDischargeAttemptExecutor attemptExecutor;
    private final DispatchApplicationService dispatchApplicationService;
    private final HospitalDispatchApplicationService hospitalDispatchApplicationService;
    private final LogicalClock logicalClock;

    public HospitalDeliveryAndDischargeApplicationService(
            EmergencyRepository emergencyRepository,
            HospitalReservationRepository hospitalReservationRepository,
            DispatchTraceabilityRepository traceabilityRepository,
            HospitalDeliveryAndDischargeAttemptExecutor attemptExecutor,
            DispatchApplicationService dispatchApplicationService,
            HospitalDispatchApplicationService hospitalDispatchApplicationService,
            LogicalClock logicalClock
    ) {
        this.emergencyRepository = Objects.requireNonNull(emergencyRepository);
        this.hospitalReservationRepository = Objects.requireNonNull(hospitalReservationRepository);
        this.traceabilityRepository = Objects.requireNonNull(traceabilityRepository);
        this.attemptExecutor = Objects.requireNonNull(attemptExecutor);
        this.dispatchApplicationService = Objects.requireNonNull(dispatchApplicationService);
        this.hospitalDispatchApplicationService = Objects.requireNonNull(hospitalDispatchApplicationService);
        this.logicalClock = Objects.requireNonNull(logicalClock);
    }

    /**
     * HU-11: Confirma la entrega hospitalaria simulada y, tras confirmar el commit atómico,
     * dispara la reevaluación de {@code WAITING_AMBULANCE} para la ambulancia liberada.
     */
    public HospitalDeliveryResult confirmHospitalDelivery(String rawEmergencyId) {
        EmergencyId emergencyId = parseEmergencyId(rawEmergencyId);
        LogicalTime now = logicalClock.currentTime();
        String operationId = "HOSP-DEL-" + UUID.randomUUID();

        HospitalDeliveryAndDischargeAttemptExecutor.DeliveryExecutionOutcome outcome =
                attemptExecutor.executeHospitalDelivery(emergencyId, now);

        if (!outcome.completed()) {
            traceabilityRepository.recordOperationResult(
                    operationId, OP_HOSPITAL_DELIVERY, emergencyId.value(),
                    outcome.hospital() != null ? outcome.hospital().id().value() : null,
                    1, "DUPLICATE_OR_INVALID_STATE", outcome.message(), now.ticks());
            return new HospitalDeliveryResult(
                    operationId,
                    HospitalDeliveryOutcome.DUPLICATE_OR_INVALID_STATE,
                    outcome.emergency(),
                    outcome.releasedAmbulance(),
                    outcome.hospital(),
                    outcome.reservation(),
                    null,
                    outcome.message()
            );
        }

        LogicalTime recordedTime = (outcome.reservation() != null && outcome.reservation().occupiedAt() != null)
                ? outcome.reservation().occupiedAt()
                : now;
        if (now.isBefore(recordedTime)) {
            logicalClock.synchronizeAtLeast(recordedTime);
        }

        traceabilityRepository.recordAttempt(
                operationId, OP_HOSPITAL_DELIVERY, emergencyId.value(), outcome.hospital().id().value(),
                1, "COMPLETED", outcome.message(), recordedTime.ticks());
        traceabilityRepository.recordOperationResult(
                operationId, OP_HOSPITAL_DELIVERY, emergencyId.value(), outcome.hospital().id().value(),
                1, "COMPLETED", outcome.message(), recordedTime.ticks());

        // Reevaluación de WAITING_AMBULANCE fuera de la transacción de entrega (ADR-0002, RN-RUT-008)
        WaitingQueueEvaluationResult queueEval = dispatchApplicationService.evaluateWaitingAmbulanceQueueForAmbulance(
                outcome.releasedAmbulance().id().value());

        return new HospitalDeliveryResult(
                operationId,
                HospitalDeliveryOutcome.COMPLETED,
                outcome.emergency(),
                outcome.releasedAmbulance(),
                outcome.hospital(),
                outcome.reservation(),
                queueEval,
                outcome.message()
        );
    }

    /**
     * HU-12: Ejecuta idempotentemente el alta simulada sobre una ocupación {@code OCCUPIED} y,
     * tras confirmar el commit, dispara la reevaluación FIFO de {@code WAITING_HOSPITAL}.
     */
    public HospitalDischargeResult executeSimulatedDischarge(String rawReservationId) {
        String reservationId = normalizeReservationId(rawReservationId);
        LogicalTime now = logicalClock.currentTime();
        String operationId = "HOSP-DIS-" + UUID.randomUUID();

        HospitalReservation snapshot = hospitalReservationRepository.findById(reservationId)
                .orElseThrow(() -> new HospitalCatalogValidationException(
                        "No existe ninguna reserva u ocupación hospitalaria con identificador " + reservationId + "."));

        HospitalDeliveryAndDischargeAttemptExecutor.DischargeExecutionOutcome outcome =
                attemptExecutor.executeSimulatedDischarge(reservationId, snapshot.hospitalId(), now);

        if (!outcome.discharged()) {
            traceabilityRepository.recordOperationResult(
                    operationId, OP_HOSPITAL_DISCHARGE, outcome.reservation().emergencyId().value(),
                    outcome.reservation().hospitalId().value(),
                    1, "DUPLICATE_OR_INVALID_STATE", outcome.message(), now.ticks());
            return new HospitalDischargeResult(
                    operationId,
                    HospitalDischargeOutcome.DUPLICATE_OR_INVALID_STATE,
                    outcome.hospital(),
                    outcome.reservation(),
                    null,
                    outcome.message()
            );
        }

        LogicalTime recordedTime = (outcome.reservation() != null && outcome.reservation().dischargedAt() != null)
                ? outcome.reservation().dischargedAt()
                : now;
        if (now.isBefore(recordedTime)) {
            logicalClock.synchronizeAtLeast(recordedTime);
        }

        traceabilityRepository.recordAttempt(
                operationId, OP_HOSPITAL_DISCHARGE, outcome.reservation().emergencyId().value(),
                outcome.hospital().id().value(), 1, "DISCHARGED", outcome.message(), recordedTime.ticks());
        traceabilityRepository.recordOperationResult(
                operationId, OP_HOSPITAL_DISCHARGE, outcome.reservation().emergencyId().value(),
                outcome.hospital().id().value(), 1, "DISCHARGED", outcome.message(), recordedTime.ticks());

        // Reevaluación FIFO de WAITING_HOSPITAL fuera de la transacción de alta (ADR-0002, RN-HOS-011, RN-HOS-013)
        WaitingHospitalQueueEvaluationResult queueEval =
                hospitalDispatchApplicationService.evaluateWaitingHospitalQueue();

        return new HospitalDischargeResult(
                operationId,
                HospitalDischargeOutcome.DISCHARGED,
                outcome.hospital(),
                outcome.reservation(),
                queueEval,
                outcome.message()
        );
    }

    @Transactional(readOnly = true)
    public List<Emergency> listInTransportEmergencies() {
        return emergencyRepository.findByStateOrderByRegisteredAtAndId(EmergencyState.IN_TRANSPORT);
    }

    @Transactional(readOnly = true)
    public List<Emergency> listCompletedEmergencies() {
        return emergencyRepository.findByStateOrderByRegisteredAtAndId(EmergencyState.COMPLETED);
    }

    @Transactional(readOnly = true)
    public List<HospitalReservation> listHospitalReservations() {
        return hospitalReservationRepository.findAllOrderByReservedAtAndId();
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

    private static String normalizeReservationId(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new HospitalCatalogValidationException("El identificador de la reserva hospitalaria es obligatorio.");
        }
        return raw.trim();
    }
}

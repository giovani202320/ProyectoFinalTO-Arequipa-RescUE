package com.arequiparescue.simulation.application;

import com.arequiparescue.dispatch.application.DispatchApplicationService;
import com.arequiparescue.dispatch.application.WaitingQueueEvaluationResult;
import com.arequiparescue.hospital.application.HospitalDeliveryAndDischargeApplicationService;
import com.arequiparescue.hospital.application.HospitalDeliveryResult;
import com.arequiparescue.hospital.application.HospitalDischargeResult;
import com.arequiparescue.hospital.application.HospitalDispatchApplicationService;
import com.arequiparescue.hospital.application.HospitalReservationResult;
import com.arequiparescue.hospital.domain.HospitalDeliveryOutcome;
import com.arequiparescue.hospital.domain.HospitalDischargeOutcome;
import com.arequiparescue.hospital.domain.HospitalDispatchOutcome;
import com.arequiparescue.simulation.domain.DiscreteEvent;
import com.arequiparescue.simulation.domain.DiscreteEventType;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * Puente de aplicación entre eventos discretos de simulación y la atención de colas, llegadas,
 * entregas hospitalarias y altas simuladas (HU-08, HU-09, HU-10, HU-11, HU-12).
 */
@Component
public class WaitingQueueSimulationTrigger {

    private final DispatchApplicationService dispatchApplicationService;
    private final HospitalDispatchApplicationService hospitalDispatchApplicationService;
    private final HospitalDeliveryAndDischargeApplicationService deliveryAndDischargeApplicationService;

    public WaitingQueueSimulationTrigger(
            DispatchApplicationService dispatchApplicationService,
            HospitalDispatchApplicationService hospitalDispatchApplicationService,
            HospitalDeliveryAndDischargeApplicationService deliveryAndDischargeApplicationService
    ) {
        this.dispatchApplicationService = Objects.requireNonNull(dispatchApplicationService);
        this.hospitalDispatchApplicationService = Objects.requireNonNull(hospitalDispatchApplicationService);
        this.deliveryAndDischargeApplicationService = Objects.requireNonNull(deliveryAndDischargeApplicationService);
    }

    public Optional<WaitingQueueEvaluationResult> handleEvent(DiscreteEvent event) {
        Objects.requireNonNull(event, "El evento discreto no puede ser nulo.");
        if (event.type() == DiscreteEventType.WAITING_QUEUE_EVALUATION) {
            WaitingQueueEvaluationResult ambulanceQueueResult =
                    dispatchApplicationService.evaluateWaitingAmbulanceQueue();
            hospitalDispatchApplicationService.evaluateWaitingHospitalQueue();
            return Optional.of(ambulanceQueueResult);
        }
        if (event.type() == DiscreteEventType.RESOURCE_RELEASE) {
            if (event.subjectId().startsWith("AMB-")) {
                return Optional.of(
                        dispatchApplicationService.evaluateWaitingAmbulanceQueueForAmbulance(event.subjectId())
                );
            }
            if (event.subjectId().startsWith("HOSP-")) {
                hospitalDispatchApplicationService.evaluateWaitingHospitalQueue();
                return Optional.empty();
            }
            if (event.subjectId().startsWith("EMG-")) {
                HospitalDeliveryResult delivery =
                        deliveryAndDischargeApplicationService.confirmHospitalDelivery(event.subjectId());
                if (delivery != null && delivery.outcome() == HospitalDeliveryOutcome.DUPLICATE_OR_INVALID_STATE) {
                    throw new IllegalStateException(delivery.message());
                }
                return Optional.ofNullable(delivery != null ? delivery.ambulanceQueueEvaluation() : null);
            }
            if (event.subjectId().startsWith("RES-")) {
                HospitalDischargeResult discharge =
                        deliveryAndDischargeApplicationService.executeSimulatedDischarge(event.subjectId());
                if (discharge != null && discharge.outcome() == HospitalDischargeOutcome.DUPLICATE_OR_INVALID_STATE) {
                    throw new IllegalStateException(discharge.message());
                }
                return Optional.empty();
            }
            throw new IllegalArgumentException(
                    "Identificador de sujeto no reconocido para RESOURCE_RELEASE: '" + event.subjectId()
                            + "'. Se esperaba prefijo AMB-, HOSP-, EMG- o RES-.");
        }
        if (event.type() == DiscreteEventType.AMBULANCE_ARRIVAL_AT_INCIDENT) {
            HospitalReservationResult arrival =
                    hospitalDispatchApplicationService.confirmArrivalAndReserveHospital(event.subjectId());
            if (arrival != null && arrival.outcome() == HospitalDispatchOutcome.DUPLICATE_OR_INVALID_STATE) {
                throw new IllegalStateException(arrival.message());
            }
            return Optional.empty();
        }
        return Optional.empty();
    }
}

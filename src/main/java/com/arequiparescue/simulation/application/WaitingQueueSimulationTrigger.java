package com.arequiparescue.simulation.application;

import com.arequiparescue.dispatch.application.DispatchApplicationService;
import com.arequiparescue.dispatch.application.WaitingQueueEvaluationResult;
import com.arequiparescue.hospital.application.HospitalDeliveryAndDischargeApplicationService;
import com.arequiparescue.hospital.application.HospitalDeliveryResult;
import com.arequiparescue.hospital.application.HospitalDispatchApplicationService;
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
                return Optional.ofNullable(delivery.ambulanceQueueEvaluation());
            }
            if (event.subjectId().startsWith("RES-")) {
                deliveryAndDischargeApplicationService.executeSimulatedDischarge(event.subjectId());
                return Optional.empty();
            }
        }
        if (event.type() == DiscreteEventType.AMBULANCE_ARRIVAL_AT_INCIDENT) {
            hospitalDispatchApplicationService.confirmArrivalAndReserveHospital(event.subjectId());
            return Optional.empty();
        }
        return Optional.empty();
    }
}

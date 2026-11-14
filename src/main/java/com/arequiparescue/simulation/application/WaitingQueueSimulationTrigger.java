package com.arequiparescue.simulation.application;

import com.arequiparescue.dispatch.application.DispatchApplicationService;
import com.arequiparescue.dispatch.application.WaitingQueueEvaluationResult;
import com.arequiparescue.hospital.application.HospitalDispatchApplicationService;
import com.arequiparescue.simulation.domain.DiscreteEvent;
import com.arequiparescue.simulation.domain.DiscreteEventType;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * Puente de aplicación entre eventos discretos de simulación y la atención de colas / llegadas
 * (HU-08, HU-09, HU-10).
 */
@Component
public class WaitingQueueSimulationTrigger {

    private final DispatchApplicationService dispatchApplicationService;
    private final HospitalDispatchApplicationService hospitalDispatchApplicationService;

    public WaitingQueueSimulationTrigger(
            DispatchApplicationService dispatchApplicationService,
            HospitalDispatchApplicationService hospitalDispatchApplicationService
    ) {
        this.dispatchApplicationService = Objects.requireNonNull(dispatchApplicationService);
        this.hospitalDispatchApplicationService = Objects.requireNonNull(hospitalDispatchApplicationService);
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
        }
        if (event.type() == DiscreteEventType.AMBULANCE_ARRIVAL_AT_INCIDENT) {
            hospitalDispatchApplicationService.confirmArrivalAndReserveHospital(event.subjectId());
            return Optional.empty();
        }
        return Optional.empty();
    }
}

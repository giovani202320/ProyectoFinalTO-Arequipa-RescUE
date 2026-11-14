package com.arequiparescue.simulation.application;

import com.arequiparescue.dispatch.application.DispatchApplicationService;
import com.arequiparescue.dispatch.application.WaitingQueueEvaluationResult;
import com.arequiparescue.simulation.domain.DiscreteEvent;
import com.arequiparescue.simulation.domain.DiscreteEventType;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * Puente de aplicación entre los eventos discretos de simulación ({@link DiscreteEventType#WAITING_QUEUE_EVALUATION}
 * y {@link DiscreteEventType#RESOURCE_RELEASE}) y la evaluación FIFO de la cola {@code WAITING_AMBULANCE} (HU-08).
 */
@Component
public class WaitingQueueSimulationTrigger {

    private final DispatchApplicationService dispatchApplicationService;

    public WaitingQueueSimulationTrigger(DispatchApplicationService dispatchApplicationService) {
        this.dispatchApplicationService = Objects.requireNonNull(dispatchApplicationService);
    }

    public Optional<WaitingQueueEvaluationResult> handleEvent(DiscreteEvent event) {
        Objects.requireNonNull(event, "El evento discreto no puede ser nulo.");
        if (event.type() == DiscreteEventType.WAITING_QUEUE_EVALUATION) {
            return Optional.of(dispatchApplicationService.evaluateWaitingAmbulanceQueue());
        }
        if (event.type() == DiscreteEventType.RESOURCE_RELEASE && event.subjectId().startsWith("AMB-")) {
            return Optional.of(
                    dispatchApplicationService.evaluateWaitingAmbulanceQueueForAmbulance(event.subjectId())
            );
        }
        return Optional.empty();
    }
}

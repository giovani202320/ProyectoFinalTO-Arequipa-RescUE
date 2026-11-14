package com.arequiparescue.simulation.application;

import com.arequiparescue.dispatch.application.DispatchApplicationService;
import com.arequiparescue.dispatch.application.WaitingQueueEvaluationResult;
import com.arequiparescue.shared.domain.LogicalTime;
import com.arequiparescue.simulation.domain.DiscreteEvent;
import com.arequiparescue.simulation.domain.DiscreteEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("WaitingQueueSimulationTrigger — Integración de eventos discretos con cola WAITING_AMBULANCE (HU-08)")
class WaitingQueueSimulationTriggerTest {

    @Test
    @DisplayName("Dispara evaluación de cola al recibir WAITING_QUEUE_EVALUATION y RESOURCE_RELEASE de ambulancia")
    void shouldTriggerQueueEvaluationOnRelevantDiscreteEvents() {
        DispatchApplicationService dispatchService = mock(DispatchApplicationService.class);
        WaitingQueueSimulationTrigger trigger = new WaitingQueueSimulationTrigger(dispatchService);

        WaitingQueueEvaluationResult expected = new WaitingQueueEvaluationResult(1, 1, 0, List.of(), "OK");
        when(dispatchService.evaluateWaitingAmbulanceQueue()).thenReturn(expected);
        when(dispatchService.evaluateWaitingAmbulanceQueueForAmbulance("AMB-01")).thenReturn(expected);

        Optional<WaitingQueueEvaluationResult> queueEval = trigger.handleEvent(new DiscreteEvent(
                "EVT-1", LogicalTime.of(3L), DiscreteEventType.WAITING_QUEUE_EVALUATION, 1L, "QUEUE", "Evaluar cola"));
        Optional<WaitingQueueEvaluationResult> releaseEval = trigger.handleEvent(new DiscreteEvent(
                "EVT-2", LogicalTime.of(4L), DiscreteEventType.RESOURCE_RELEASE, 2L, "AMB-01", "Ambulancia libre"));

        assertThat(queueEval).isPresent();
        assertThat(releaseEval).isPresent();
        verify(dispatchService).evaluateWaitingAmbulanceQueue();
        verify(dispatchService).evaluateWaitingAmbulanceQueueForAmbulance("AMB-01");
    }

    @Test
    @DisplayName("Ignora eventos discretos ajenos a evaluación de cola o liberación de ambulancia")
    void shouldIgnoreUnrelatedDiscreteEvents() {
        DispatchApplicationService dispatchService = mock(DispatchApplicationService.class);
        WaitingQueueSimulationTrigger trigger = new WaitingQueueSimulationTrigger(dispatchService);

        Optional<WaitingQueueEvaluationResult> result = trigger.handleEvent(new DiscreteEvent(
                "EVT-3", LogicalTime.of(1L), DiscreteEventType.EMERGENCY_REGISTRATION, 1L, "EMG-00001", "Registro"));

        assertThat(result).isEmpty();
        verifyNoInteractions(dispatchService);
    }
}

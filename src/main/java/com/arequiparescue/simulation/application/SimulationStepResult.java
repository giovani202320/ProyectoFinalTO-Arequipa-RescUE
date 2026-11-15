package com.arequiparescue.simulation.application;

import com.arequiparescue.dispatch.application.WaitingQueueEvaluationResult;
import com.arequiparescue.shared.domain.LogicalTime;
import com.arequiparescue.simulation.domain.ScheduledSimulationEvent;

/**
 * Resumen trazable del avance manual del reloj lógico hacia el siguiente evento discreto (HU-13).
 */
public record SimulationStepResult(
        String scenarioId,
        SimulationStepOutcome outcome,
        LogicalTime previousTime,
        LogicalTime currentTime,
        ScheduledSimulationEvent processedEvent,
        WaitingQueueEvaluationResult waitingQueueResult,
        String message
) {
}

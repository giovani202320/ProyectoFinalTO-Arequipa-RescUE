package com.arequiparescue.dispatch.application;

import java.util.List;

/**
 * Resultado consolidado de un ciclo de evaluación FIFO sobre la cola {@code WAITING_AMBULANCE} (HU-08).
 */
public record WaitingQueueEvaluationResult(
        int evaluatedAvailableAmbulances,
        int assignedCount,
        int remainingWaitingCount,
        List<AutomaticDispatchResult> assignments,
        String summaryMessage
) {
}

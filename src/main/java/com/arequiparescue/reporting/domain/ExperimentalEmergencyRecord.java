package com.arequiparescue.reporting.domain;

import java.util.List;

/**
 * Registro funcional experimental mínimo y trazable por emergencia ({@code RN-MET-003}, {@code CA-SIM-10}).
 */
public record ExperimentalEmergencyRecord(
        String emergencyId,
        String currentState,
        String ruleVersionId,
        String assignedAmbulanceId,
        String hospitalId,
        String lastOutcomeReason,
        long registeredAtTicks,
        Long queueEnteredAtTicks,
        Long assignedAtTicks,
        Long atIncidentAtTicks,
        Long hospitalQueueEnteredAtTicks,
        Long inTransportAtTicks,
        Long completedAtTicks,
        Long dischargedAtTicks,
        List<OperationResultTrace> operationResults,
        List<AttemptLogTrace> attemptLogs
) {

    public record OperationResultTrace(
            String operationId,
            String operationType,
            String assignedResourceId,
            int totalAttempts,
            String finalStatus,
            String outcomeReason,
            long logicalTicks
    ) {
    }

    public record AttemptLogTrace(
            String operationId,
            String operationType,
            String candidateResourceId,
            int attemptNumber,
            String outcomeCode,
            String detailMessage,
            long logicalTicks
    ) {
    }
}

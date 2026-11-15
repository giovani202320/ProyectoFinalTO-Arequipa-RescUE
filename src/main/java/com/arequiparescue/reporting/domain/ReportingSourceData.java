package com.arequiparescue.reporting.domain;

import java.util.List;

/**
 * Datos crudos de solo lectura proyectados desde la persistencia operacional para calcular
 * el reporte experimental de {@code HU-14} sin acoplarse a infraestructura externa.
 */
public record ReportingSourceData(
        String referenceScenarioId,
        String referenceScenarioStatus,
        long currentLogicalTicks,
        List<ScenarioRow> scenarios,
        List<EmergencyRow> emergencies,
        List<AmbulanceRow> ambulances,
        List<HospitalReservationRow> hospitalReservations,
        List<OperationResultRow> operationResults,
        List<AttemptLogRow> attemptLogs
) {

    public record ScenarioRow(
            String id,
            String name,
            String status,
            long currentLogicalTicks
    ) {
    }

    public record EmergencyRow(
            String id,
            String state,
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
            Long completedAtTicks
    ) {
    }

    public record AmbulanceRow(
            String id,
            String equipmentType,
            String state,
            String assignedEmergencyId
    ) {
    }

    public record HospitalReservationRow(
            String emergencyId,
            String hospitalId,
            String status,
            Long dischargedAtTicks
    ) {
    }

    public record OperationResultRow(
            String operationId,
            String operationType,
            String emergencyId,
            String assignedResourceId,
            int totalAttempts,
            String finalStatus,
            String outcomeReason,
            long logicalTicks
    ) {
    }

    public record AttemptLogRow(
            String operationId,
            String operationType,
            String emergencyId,
            String candidateResourceId,
            int attemptNumber,
            String outcomeCode,
            String detailMessage,
            long logicalTicks
    ) {
    }
}

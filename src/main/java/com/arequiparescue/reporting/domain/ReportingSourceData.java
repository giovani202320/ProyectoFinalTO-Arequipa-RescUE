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
        List<AttemptLogRow> attemptLogs,
        List<HospitalRow> hospitals
) {

    public ReportingSourceData(
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
        this(referenceScenarioId, referenceScenarioStatus, currentLogicalTicks, scenarios,
                emergencies, ambulances, hospitalReservations, operationResults, attemptLogs, List.of());
    }

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
            Long completedAtTicks,
            String simulatedPriority,
            String incidentCategory
    ) {
        public EmergencyRow(
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
            this(id, state, ruleVersionId, assignedAmbulanceId, hospitalId, lastOutcomeReason,
                    registeredAtTicks, queueEnteredAtTicks, assignedAtTicks, atIncidentAtTicks,
                    hospitalQueueEnteredAtTicks, inTransportAtTicks, completedAtTicks, null, null);
        }
    }

    public record HospitalRow(
            String id,
            String name,
            String nodeId,
            boolean enabled,
            int capacityTotal,
            int reservedSlots,
            int occupiedSlots
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

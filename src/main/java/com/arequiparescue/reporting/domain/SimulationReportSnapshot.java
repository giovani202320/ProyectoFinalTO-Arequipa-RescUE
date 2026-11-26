package com.arequiparescue.reporting.domain;

import java.util.List;

/**
 * Instantánea de solo lectura del reporte experimental completo de {@code HU-14}:
 * catálogo de 7 métricas aprobadas ({@code RN-MET-004}) y registro funcional trazable ({@code RN-MET-003}).
 */
public record SimulationReportSnapshot(
        String referenceScenarioId,
        String referenceScenarioStatus,
        long currentLogicalTicks,
        TemporalMetricSummary waitingForAmbulance,
        TemporalMetricSummary responseToIncident,
        TemporalMetricSummary pickupToHospital,
        CompletedEmergenciesMetric completedEmergencies,
        AmbulanceUtilizationMetric ambulanceUtilization,
        ConcurrencyMetric concurrencyConflicts,
        TemporalMetricSummary waitingForHospital,
        List<ExperimentalEmergencyRecord> experimentalRecords,
        ReportingSupplementaryMetrics.EmergencyDistributionSummary emergencyDistribution,
        ReportingSupplementaryMetrics.HospitalCapacitySummary hospitalCapacity,
        ReportingSupplementaryMetrics.OperationOutcomeSummary operationOutcomes
) {

    public SimulationReportSnapshot(
            String referenceScenarioId,
            String referenceScenarioStatus,
            long currentLogicalTicks,
            TemporalMetricSummary waitingForAmbulance,
            TemporalMetricSummary responseToIncident,
            TemporalMetricSummary pickupToHospital,
            CompletedEmergenciesMetric completedEmergencies,
            AmbulanceUtilizationMetric ambulanceUtilization,
            ConcurrencyMetric concurrencyConflicts,
            TemporalMetricSummary waitingForHospital,
            List<ExperimentalEmergencyRecord> experimentalRecords
    ) {
        this(referenceScenarioId, referenceScenarioStatus, currentLogicalTicks,
                waitingForAmbulance, responseToIncident, pickupToHospital,
                completedEmergencies, ambulanceUtilization, concurrencyConflicts,
                waitingForHospital, experimentalRecords,
                ReportingSupplementaryMetrics.EmergencyDistributionSummary.empty(),
                ReportingSupplementaryMetrics.HospitalCapacitySummary.empty(),
                ReportingSupplementaryMetrics.OperationOutcomeSummary.empty());
    }
}

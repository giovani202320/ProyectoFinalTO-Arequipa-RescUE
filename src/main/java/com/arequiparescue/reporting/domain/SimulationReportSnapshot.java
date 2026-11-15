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
        List<ExperimentalEmergencyRecord> experimentalRecords
) {
}

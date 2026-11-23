package com.arequiparescue.reporting.domain;

import java.util.List;
import java.util.Map;

/**
 * Modelos de solo lectura para los indicadores complementarios del panel de reportes (P1-2):
 * distribución de emergencias, capacidad hospitalaria C/R/O/D, resultados de operaciones
 * y filtro de trazabilidad.
 */
public final class ReportingSupplementaryMetrics {

    private ReportingSupplementaryMetrics() {
    }

    public record DistributionBucket(
            String key,
            long activeCount,
            long completedCount,
            long totalCount
    ) {
    }

    public record EmergencyDistributionSummary(
            long totalEmergencies,
            long activeEmergencies,
            long completedEmergencies,
            List<DistributionBucket> byPriority,
            List<DistributionBucket> byCategory,
            boolean totalsConsistent
    ) {
        public static EmergencyDistributionSummary empty() {
            return new EmergencyDistributionSummary(0L, 0L, 0L, List.of(), List.of(), true);
        }
    }

    public record HospitalCapacityItem(
            String hospitalId,
            String name,
            String nodeId,
            boolean enabled,
            int totalCapacity,
            int reservedSlots,
            int occupiedSlots,
            int availableSlots,
            boolean invariantValid
    ) {
    }

    public record HospitalCapacitySummary(
            int totalHospitals,
            int enabledHospitals,
            int disabledHospitals,
            int totalCapacity,
            int totalReserved,
            int totalOccupied,
            int totalAvailable,
            int enabledAvailable,
            boolean allInvariantsValid,
            List<HospitalCapacityItem> items
    ) {
        public static HospitalCapacitySummary empty() {
            return new HospitalCapacitySummary(0, 0, 0, 0, 0, 0, 0, 0, true, List.of());
        }
    }

    public record OperationOutcomeSummary(
            long totalFinalOperations,
            long successfulCount,
            long deferredToQueueCount,
            long rejectedOrFailedCount,
            long otherFinalStatusCount,
            long totalAttemptLogs,
            Map<String, Long> finalStatusBreakdown,
            Map<String, Long> emergencyOutcomeReasons
    ) {
        public static OperationOutcomeSummary empty() {
            return new OperationOutcomeSummary(0L, 0L, 0L, 0L, 0L, 0L, Map.of(), Map.of());
        }
    }

    public record TraceabilityFilter(
            String state,
            String priority,
            Long fromTicks,
            Long toTicks
    ) {
        public static TraceabilityFilter none() {
            return new TraceabilityFilter(null, null, null, null);
        }

        public TraceabilityFilter {
            state = (state == null || state.isBlank()) ? null : state.trim();
            priority = (priority == null || priority.isBlank()) ? null : priority.trim();
            if (fromTicks != null && fromTicks < 0L) {
                throw new IllegalArgumentException("El tiempo lógico inicial (t desde) no puede ser negativo.");
            }
            if (toTicks != null && toTicks < 0L) {
                throw new IllegalArgumentException("El tiempo lógico final (t hasta) no puede ser negativo.");
            }
            if (fromTicks != null && toTicks != null && fromTicks > toTicks) {
                throw new IllegalArgumentException(
                        "El rango de tiempo lógico es inválido: t desde (" + fromTicks + ") no puede superar a t hasta (" + toTicks + ").");
            }
        }
    }
}

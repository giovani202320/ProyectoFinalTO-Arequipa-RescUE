package com.arequiparescue.reporting.domain;

import java.util.Map;

/**
 * Métrica 4 de {@code RN-MET-004}: número de emergencias completadas ({@code COMPLETED})
 * y desglose separado de emergencias activas/en curso por estado.
 */
public record CompletedEmergenciesMetric(
        long completedCount,
        long activeCount,
        Map<String, Long> activeCountsByState
) {
}

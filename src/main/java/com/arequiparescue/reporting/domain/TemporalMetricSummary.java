package com.arequiparescue.reporting.domain;

import java.util.List;

/**
 * Representa una medición temporal agregada del catálogo {@code RN-MET-004} (métricas 1, 2, 3 y 7),
 * separando estrictamente los casos cerrados de los casos aún en curso para observar <em>starvation</em>
 * ({@code CA-SIM-08}).
 */
public record TemporalMetricSummary(
        String code,
        String name,
        String unit,
        long closedCount,
        Double closedAverageTicks,
        long inProgressCount,
        String inProgressStatusLabel,
        List<InProgressTemporalItem> inProgressItems
) {

    public record InProgressTemporalItem(
            String emergencyId,
            long startedAtTicks,
            long accumulatedTicks
    ) {
    }
}

package com.arequiparescue.reporting.domain;

import java.util.List;

/**
 * Métrica 5 de {@code RN-MET-004}: utilización de ambulancias en unidades de tiempo lógico
 * ocupadas y proporción sobre el tiempo lógico transcurrido del escenario ({@code \Delta t > 0}).
 */
public record AmbulanceUtilizationMetric(
        long scenarioElapsedTicks,
        long totalOccupiedTicks,
        double averageUtilizationRatio,
        List<AmbulanceUtilizationItem> items
) {

    public record AmbulanceUtilizationItem(
            String ambulanceId,
            String equipmentType,
            String currentState,
            long occupiedTicks,
            double utilizationRatio
    ) {
    }
}

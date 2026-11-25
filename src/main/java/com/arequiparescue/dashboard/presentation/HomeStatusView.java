package com.arequiparescue.dashboard.presentation;

import java.util.List;

/**
 * DTO de presentación para el centro de control operativo de inicio ({@code GET /}, P1-3).
 * Adaptado en {@link HomeController} a partir del snapshot de aplicación.
 */
public record HomeStatusView(
        String systemTitle,
        String academicDescription,
        String architectureSummary,
        String environmentProfileLabel,
        String environmentProfileTone,
        String environmentProfileNote,
        long globalLogicalTicks,
        boolean hasActiveScenario,
        String activeScenarioId,
        String activeScenarioName,
        Long activeScenarioLogicalTicks,
        long totalEmergencies,
        long activeEmergencies,
        long completedEmergencies,
        long waitingAmbulanceCount,
        long waitingHospitalCount,
        List<StateDistributionRow> stateDistribution,
        boolean stateDistributionConsistent,
        long totalAmbulances,
        long availableAmbulances,
        long occupiedAmbulances,
        int totalHospitals,
        int enabledHospitals,
        int disabledHospitals,
        int enabledCapacityTotal,
        int enabledReservedSlots,
        int enabledOccupiedSlots,
        int enabledAvailableCapacity,
        boolean hospitalCapacityConsistent,
        int inconsistentHospitalsCount,
        boolean emptyDatabase,
        List<OperationalNoticeView> notices
) {

    public record StateDistributionRow(
            String stateCode,
            long count,
            double sharePercent,
            String barToneClass
    ) {
    }

    public record OperationalNoticeView(
            String alertClass,
            String message,
            String actionLabel,
            String actionPath
    ) {
    }
}

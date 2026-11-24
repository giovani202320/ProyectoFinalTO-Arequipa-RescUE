package com.arequiparescue.shared.application;

import java.util.List;

/**
 * Snapshot inmutable de aplicación con el estado operativo actual del simulador ARE
 * para el panel de inicio (P1-3). No depende de la capa de presentación ni de JPA.
 */
public record HomeDashboardSnapshot(
        EnvironmentProfileInfo environmentProfile,
        long globalLogicalTicks,
        ActiveScenarioInfo activeScenario,
        long totalEmergencies,
        long activeEmergencies,
        long completedEmergencies,
        long waitingAmbulanceCount,
        long waitingHospitalCount,
        List<EmergencyStateCount> stateDistribution,
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
        List<OperationalNotice> notices
) {

    public enum EnvironmentProfileKind {
        DEMO,
        HISTORICAL,
        MYSQL_TEST,
        CUSTOM,
        UNIDENTIFIED
    }

    public record EnvironmentProfileInfo(
            EnvironmentProfileKind kind,
            String displayLabel,
            String detailNote
    ) {
    }

    public record ActiveScenarioInfo(
            String scenarioId,
            String name,
            long scenarioLogicalTicks
    ) {
    }

    public record EmergencyStateCount(
            String stateCode,
            long count,
            double sharePercent
    ) {
    }

    public record OperationalNotice(
            String severity,
            String message,
            String actionLabel,
            String actionPath
    ) {
    }
}

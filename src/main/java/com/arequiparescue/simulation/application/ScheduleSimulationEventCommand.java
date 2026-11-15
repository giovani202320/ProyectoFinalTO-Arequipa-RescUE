package com.arequiparescue.simulation.application;

import com.arequiparescue.simulation.domain.DiscreteEventType;

/**
 * Comando de aplicación para programar un evento discreto en un escenario experimental (HU-13).
 */
public record ScheduleSimulationEventCommand(
        String scenarioId,
        String eventId,
        long scheduledAtTicks,
        DiscreteEventType eventType,
        String subjectId,
        String description
) {
}

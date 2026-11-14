package com.arequiparescue.emergency.domain;

import java.util.Objects;

/**
 * Regla individual de clasificación asociada a una categoría de incidente dentro de una versión
 * inmutable de reglas (DEC-016, RN-EME-007, RN-EME-011).
 */
public record TriageRuleEntry(
        IncidentCategory category,
        SimulatedPriority simulatedPriority,
        EquipmentType requiredEquipment,
        boolean active
) {

    public TriageRuleEntry {
        Objects.requireNonNull(category, "La categoría del incidente en la regla no puede ser nula.");
        Objects.requireNonNull(simulatedPriority, "La prioridad simulada en la regla no puede ser nula.");
        Objects.requireNonNull(requiredEquipment, "El equipamiento requerido en la regla no puede ser nulo.");
    }
}

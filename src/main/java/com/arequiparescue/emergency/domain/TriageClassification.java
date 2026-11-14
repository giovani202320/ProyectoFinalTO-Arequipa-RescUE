package com.arequiparescue.emergency.domain;

import java.util.Objects;

/**
 * Resultado inmutable y determinista de evaluar una categoría de incidente contra una versión
 * activa de reglas de triage (DEC-016, RN-EME-003, RN-EME-004, RN-EME-007).
 */
public record TriageClassification(
        IncidentCategory category,
        SimulatedPriority simulatedPriority,
        EquipmentType requiredEquipment,
        TriageRuleVersionId appliedRuleVersionId
) {

    public TriageClassification {
        Objects.requireNonNull(category, "La categoría clasificada no puede ser nula.");
        Objects.requireNonNull(simulatedPriority, "La prioridad simulada determinada no puede ser nula.");
        Objects.requireNonNull(requiredEquipment, "El equipamiento requerido determinado no puede ser nulo.");
        Objects.requireNonNull(appliedRuleVersionId, "La referencia a la versión de reglas aplicada no puede ser nula.");
    }
}

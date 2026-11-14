package com.arequiparescue.emergency.domain;

import java.util.Objects;

/**
 * Servicio de dominio puro para ejecutar la clasificación simulada determinista de emergencias
 * contra la versión activa de reglas (DEC-010, DEC-016, RN-EME-002 a RN-EME-010).
 */
public final class DeterministicTriageService {

    /**
     * Valida el código de categoría introducido y evalúa de forma reproducible su prioridad informativa
     * y equipamiento requerido contra la versión activa de reglas.
     */
    public TriageClassification classify(String rawCategoryCode, TriageRuleVersion activeVersion) {
        Objects.requireNonNull(
                activeVersion,
                "Debe existir una versión activa de reglas para clasificar la emergencia (RN-EME-010.5, RN-EME-011.1)."
        );
        IncidentCategory category = IncidentCategory.fromCode(rawCategoryCode);
        return classify(category, activeVersion);
    }

    public TriageClassification classify(IncidentCategory category, TriageRuleVersion activeVersion) {
        Objects.requireNonNull(category, "La categoría del incidente es obligatoria (RN-EME-010.1).");
        Objects.requireNonNull(
                activeVersion,
                "Debe existir una versión activa de reglas para clasificar la emergencia (RN-EME-010.5, RN-EME-011.1)."
        );
        return activeVersion.classify(category);
    }
}

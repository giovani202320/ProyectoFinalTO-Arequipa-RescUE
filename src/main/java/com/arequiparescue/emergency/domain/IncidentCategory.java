package com.arequiparescue.emergency.domain;

import java.util.Arrays;

/**
 * Catálogo inicial aprobado de categorías ficticias de incidente para el simulador académico ARE
 * (DEC-016, RN-EME-007, RN-EME-010).
 */
public enum IncidentCategory {

    INC_A("INC-A", "INC-A — Emergencia simulada crítica con requerimiento especializado"),
    INC_B("INC-B", "INC-B — Emergencia simulada de prioridad media con requerimiento básico"),
    INC_C("INC-C", "INC-C — Emergencia simulada de prioridad baja con requerimiento básico"),
    INC_D("INC-D", "INC-D — Emergencia simulada de prioridad alta con requerimiento básico");

    private final String code;
    private final String displayLabel;

    IncidentCategory(String code, String displayLabel) {
        this.code = code;
        this.displayLabel = displayLabel;
    }

    public String code() {
        return code;
    }

    public String displayLabel() {
        return displayLabel;
    }

    /**
     * Resuelve de forma determinista el código de categoría introducido, rechazando valores nulos,
     * vacíos o fuera del catálogo aprobado (RN-EME-010.1, RN-EME-010.2).
     */
    public static IncidentCategory fromCode(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new IllegalArgumentException("La categoría del incidente es obligatoria (RN-EME-010).");
        }
        String normalized = rawCode.trim();
        return Arrays.stream(values())
                .filter(category -> category.code.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "La categoría del incidente no existe o no pertenece al catálogo activo aprobado (INC-A, INC-B, INC-C, INC-D): "
                                + normalized));
    }
}

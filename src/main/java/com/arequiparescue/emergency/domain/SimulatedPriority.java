package com.arequiparescue.emergency.domain;

/**
 * Prioridad simulada informativa determinada por el triage determinista (DEC-016, RN-EME-007, RN-EME-008).
 * Su carácter es exclusivamente informativo y no altera el orden FIFO ni las reglas de compatibilidad.
 */
public enum SimulatedPriority {

    HIGH("Alta"),
    MEDIUM("Media"),
    LOW("Baja");

    private final String displayLabel;

    SimulatedPriority(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public String displayLabel() {
        return displayLabel;
    }
}

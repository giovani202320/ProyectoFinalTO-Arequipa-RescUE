package com.arequiparescue.emergency.domain;

import java.util.Objects;

/**
 * Categoría de equipamiento requerido por una emergencia o provisto por una ambulancia
 * (DEC-016, RN-EME-007).
 */
public enum EquipmentType {

    BASIC("Básico"),
    SPECIALIZED("Especializado");

    private final String displayLabel;

    EquipmentType(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public String displayLabel() {
        return displayLabel;
    }

    /**
     * Evalúa si una unidad con este equipamiento puede satisfacer el equipamiento requerido
     * por una emergencia (RN-EME-007: SPECIALIZED cubre BASIC y SPECIALIZED; BASIC solo cubre BASIC).
     */
    public boolean canSatisfy(EquipmentType required) {
        Objects.requireNonNull(required, "El equipamiento requerido a evaluar no puede ser nulo.");
        if (this == SPECIALIZED) {
            return true;
        }
        return required == BASIC;
    }
}

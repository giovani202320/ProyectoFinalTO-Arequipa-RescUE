package com.arequiparescue.shared.domain;

/**
 * Excepción base abstracta para validaciones funcionales asociadas a un campo específico
 * de entrada en formularios y casos de uso de ARE (B1).
 * Mantiene semántica unchecked ({@link RuntimeException}) compatible con rollback transaccional.
 */
public abstract class FieldValidationException extends RuntimeException {

    private final String field;

    protected FieldValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}

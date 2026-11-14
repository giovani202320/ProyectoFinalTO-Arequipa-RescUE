package com.arequiparescue.emergency.application;

/**
 * Excepción de validación funcional durante el registro de una emergencia (RN-EME-010).
 */
public class EmergencyValidationException extends RuntimeException {

    private final String field;

    public EmergencyValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}

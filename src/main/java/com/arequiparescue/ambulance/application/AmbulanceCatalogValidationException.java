package com.arequiparescue.ambulance.application;

/**
 * Excepción de validación funcional durante la administración del catálogo de ambulancias (HU-03).
 */
public class AmbulanceCatalogValidationException extends RuntimeException {

    private final String field;

    public AmbulanceCatalogValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}

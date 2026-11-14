package com.arequiparescue.hospital.application;

/**
 * Excepción de validación funcional durante la administración del catálogo de hospitales (HU-03).
 */
public class HospitalCatalogValidationException extends RuntimeException {

    private final String field;

    public HospitalCatalogValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}

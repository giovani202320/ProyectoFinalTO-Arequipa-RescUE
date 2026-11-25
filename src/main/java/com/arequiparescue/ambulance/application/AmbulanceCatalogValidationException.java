package com.arequiparescue.ambulance.application;

import com.arequiparescue.shared.domain.FieldValidationException;

/**
 * Excepción de validación funcional durante la administración del catálogo de ambulancias (HU-03).
 */
public class AmbulanceCatalogValidationException extends FieldValidationException {

    public AmbulanceCatalogValidationException(String field, String message) {
        super(field, message);
    }
}

package com.arequiparescue.hospital.application;

import com.arequiparescue.shared.domain.FieldValidationException;

/**
 * Excepción de validación funcional durante la administración del catálogo de hospitales (HU-03).
 */
public class HospitalCatalogValidationException extends FieldValidationException {

    public HospitalCatalogValidationException(String message) {
        super(null, message);
    }

    public HospitalCatalogValidationException(String field, String message) {
        super(field, message);
    }
}

package com.arequiparescue.emergency.application;

import com.arequiparescue.shared.domain.FieldValidationException;

/**
 * Excepción de validación funcional durante el registro de una emergencia (RN-EME-010).
 */
public class EmergencyValidationException extends FieldValidationException {

    public EmergencyValidationException(String field, String message) {
        super(field, message);
    }
}

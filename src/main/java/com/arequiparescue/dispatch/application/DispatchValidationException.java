package com.arequiparescue.dispatch.application;

/**
 * Excepción de validación o precondición no cumplida en operaciones de despacho (HU-05 y HU-06).
 */
public class DispatchValidationException extends RuntimeException {

    public DispatchValidationException(String message) {
        super(message);
    }
}

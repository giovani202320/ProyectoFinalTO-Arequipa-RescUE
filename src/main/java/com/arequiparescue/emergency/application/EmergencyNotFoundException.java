package com.arequiparescue.emergency.application;

/**
 * Excepción lanzada cuando se solicita consultar una emergencia cuyo identificador no existe (HU-02).
 */
public class EmergencyNotFoundException extends RuntimeException {

    public EmergencyNotFoundException(String emergencyId) {
        super("No se encontró ninguna emergencia registrada con el identificador: " + emergencyId);
    }
}

package com.arequiparescue.emergency.application;

/**
 * Comando de entrada del caso de uso de registro y clasificación simulada de emergencia (HU-01).
 * Contiene exclusivamente los tres campos introducidos por el Operador de recepción (RN-EME-009).
 */
public record RegisterEmergencyCommand(
        String incidentCategoryCode,
        String locationNodeCode,
        String description
) {
}

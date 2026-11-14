package com.arequiparescue.ambulance.application;

/**
 * Comando para registrar una ambulancia en el catálogo sobre un nodo válido N01-N10 (HU-03 CA-1).
 */
public record RegisterAmbulanceCommand(
        String id,
        String equipmentTypeCode,
        String nodeIdCode
) {
}

package com.arequiparescue.ambulance.application;

/**
 * Comando para actualizar equipamiento y nodo de una ambulancia del catálogo (HU-03 CA-1).
 */
public record UpdateAmbulanceCommand(
        String id,
        String equipmentTypeCode,
        String nodeIdCode
) {
}

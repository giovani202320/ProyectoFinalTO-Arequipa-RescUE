package com.arequiparescue.hospital.application;

/**
 * Comando para actualizar un hospital del catálogo preservando R + O <= C y el bloqueo durante
 * escenarios experimentales activos (HU-03 CA-1, CA-2, CA-3, RN-HOS-015, CA-HOS-12).
 */
public record UpdateHospitalCommand(
        String id,
        String name,
        String nodeIdCode,
        boolean enabled,
        Integer totalCapacity
) {
}

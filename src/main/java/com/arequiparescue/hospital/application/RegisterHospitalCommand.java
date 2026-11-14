package com.arequiparescue.hospital.application;

/**
 * Comando para registrar un hospital en el catálogo sobre un nodo N01-N10 con capacidad simulada C >= 0
 * sin especialidades clínicas (HU-03 CA-1, DEC-018, RN-HOS-004).
 */
public record RegisterHospitalCommand(
        String id,
        String name,
        String nodeIdCode,
        boolean enabled,
        Integer totalCapacity
) {
}

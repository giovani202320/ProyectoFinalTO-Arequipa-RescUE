package com.arequiparescue.hospital.presentation;

import com.arequiparescue.hospital.domain.Hospital;

/**
 * Modelo de presentación (DTO) de un hospital en el catálogo (HU-03, DEC-018, RN-HOS-004).
 */
public record HospitalView(
        String id,
        String name,
        String nodeId,
        boolean enabled,
        int totalCapacity,
        int reservedSlots,
        int occupiedSlots,
        int availableSlots
) {

    public static HospitalView fromDomain(Hospital hospital) {
        return new HospitalView(
                hospital.id().value(),
                hospital.name(),
                hospital.locationNode().value(),
                hospital.enabled(),
                hospital.totalCapacity(),
                hospital.reservedSlots(),
                hospital.occupiedSlots(),
                hospital.availableSlots()
        );
    }
}

package com.arequiparescue.ambulance.presentation;

import com.arequiparescue.ambulance.domain.Ambulance;

/**
 * DTO de presentación para visualizar una ambulancia del catálogo (HU-03).
 */
public record AmbulanceView(
        String id,
        String equipmentTypeCode,
        String equipmentTypeLabel,
        String currentNodeId,
        String stateCode,
        String stateLabel,
        String assignedEmergencyId
) {

    public static AmbulanceView fromDomain(Ambulance ambulance) {
        return new AmbulanceView(
                ambulance.id().value(),
                ambulance.equipmentType().name(),
                ambulance.equipmentType().displayLabel(),
                ambulance.currentNode().value(),
                ambulance.state().name(),
                ambulance.state().displayLabel(),
                ambulance.assignedEmergencyId() == null ? null : ambulance.assignedEmergencyId().value()
        );
    }
}

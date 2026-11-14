package com.arequiparescue.emergency.presentation;

import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.shared.domain.LogicalTime;

/**
 * Modelo de vista inmutable para presentar una emergencia registrada en el listado y detalle (HU-01, HU-02)
 * sin exponer entidades de dominio ni de persistencia JPA a la capa de vistas (DEC-002, DEC-003, AGENTS.md Secc. 5.2).
 */
public record EmergencyView(
        String id,
        String incidentCategoryCode,
        String incidentCategoryLabel,
        String nodeId,
        String description,
        String simulatedPriorityCode,
        String simulatedPriorityLabel,
        String requiredEquipmentCode,
        String requiredEquipmentLabel,
        String appliedRuleVersionId,
        String stateCode,
        String stateLabel,
        String stateOperationalDescription,
        String assignedAmbulanceId,
        String reservedOrOccupiedHospitalId,
        long registeredAtTicks,
        Long queueEnteredAtTicks,
        Long assignedAtTicks,
        Long atIncidentAtTicks,
        Long hospitalQueueEnteredAtTicks,
        Long inTransportAtTicks,
        Long completedAtTicks,
        boolean retainsAmbulanceAtIncidentWaitingHospital,
        boolean cycleClosed
) {

    public static EmergencyView fromDomain(Emergency emergency) {
        return new EmergencyView(
                emergency.id().value(),
                emergency.category().code(),
                emergency.category().displayLabel(),
                emergency.locationNode().value(),
                emergency.description(),
                emergency.simulatedPriority().name(),
                emergency.simulatedPriority().displayLabel(),
                emergency.requiredEquipment().name(),
                emergency.requiredEquipment().displayLabel(),
                emergency.appliedRuleVersionId().value(),
                emergency.state().name(),
                emergency.state().displayLabel(),
                emergency.state().operationalDescription(),
                emergency.assignedAmbulanceId(),
                emergency.reservedOrOccupiedHospitalId(),
                emergency.registeredAt().ticks(),
                toNullableTicks(emergency.queueEnteredAt()),
                toNullableTicks(emergency.assignedAt()),
                toNullableTicks(emergency.atIncidentAt()),
                toNullableTicks(emergency.hospitalQueueEnteredAt()),
                toNullableTicks(emergency.inTransportAt()),
                toNullableTicks(emergency.completedAt()),
                emergency.retainsAmbulanceAtIncidentWaitingHospital(),
                emergency.isCycleClosed()
        );
    }

    private static Long toNullableTicks(LogicalTime time) {
        return time == null ? null : time.ticks();
    }
}

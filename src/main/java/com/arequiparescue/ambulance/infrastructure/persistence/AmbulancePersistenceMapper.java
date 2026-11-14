package com.arequiparescue.ambulance.infrastructure.persistence;

import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.ambulance.domain.AmbulanceId;
import com.arequiparescue.ambulance.domain.AmbulanceState;
import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.emergency.domain.EquipmentType;
import com.arequiparescue.routing.domain.NodeId;

import java.util.Objects;

/**
 * Mapeador explícito entre {@link Ambulance} (dominio puro) y {@link AmbulanceJpaEntity} (persistencia JPA)
 * preservando las invariantes de DEC-019 e INV-SM-02 (DEC-003, ADR-0001 Secc. 4.3.1).
 */
public final class AmbulancePersistenceMapper {

    private AmbulancePersistenceMapper() {
    }

    public static AmbulanceJpaEntity toJpaEntity(Ambulance ambulance) {
        Objects.requireNonNull(ambulance, "La entidad de dominio Ambulance a mapear no puede ser nula.");
        return new AmbulanceJpaEntity(
                ambulance.id().value(),
                ambulance.equipmentType().name(),
                ambulance.currentNode().value(),
                ambulance.state().name(),
                ambulance.assignedEmergencyId() == null ? null : ambulance.assignedEmergencyId().value()
        );
    }

    public static Ambulance toDomain(AmbulanceJpaEntity entity) {
        Objects.requireNonNull(entity, "La entidad JPA AmbulanceJpaEntity a mapear no puede ser nula.");
        return Ambulance.reconstruct(
                AmbulanceId.of(entity.getId()),
                EquipmentType.valueOf(entity.getEquipmentType()),
                NodeId.of(entity.getCurrentNodeId()),
                AmbulanceState.valueOf(entity.getState()),
                entity.getAssignedEmergencyId() == null ? null : EmergencyId.of(entity.getAssignedEmergencyId())
        );
    }
}

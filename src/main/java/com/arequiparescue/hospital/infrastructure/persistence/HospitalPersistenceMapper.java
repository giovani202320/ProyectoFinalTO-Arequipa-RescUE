package com.arequiparescue.hospital.infrastructure.persistence;

import com.arequiparescue.hospital.domain.Hospital;
import com.arequiparescue.hospital.domain.HospitalId;
import com.arequiparescue.routing.domain.NodeId;

import java.util.Objects;

/**
 * Mapeador explícito entre {@link Hospital} (dominio puro) y {@link HospitalJpaEntity} (persistencia JPA)
 * preservando las invariantes de capacidad C, R, O, D (DEC-003, DEC-018, ADR-0001 Secc. 4.3.1).
 */
public final class HospitalPersistenceMapper {

    private HospitalPersistenceMapper() {
    }

    public static HospitalJpaEntity toJpaEntity(Hospital hospital) {
        Objects.requireNonNull(hospital, "La entidad de dominio Hospital a mapear no puede ser nula.");
        return new HospitalJpaEntity(
                hospital.id().value(),
                hospital.name(),
                hospital.locationNode().value(),
                hospital.enabled(),
                hospital.totalCapacity(),
                hospital.reservedSlots(),
                hospital.occupiedSlots()
        );
    }

    public static Hospital toDomain(HospitalJpaEntity entity) {
        Objects.requireNonNull(entity, "La entidad JPA HospitalJpaEntity a mapear no puede ser nula.");
        return Hospital.reconstruct(
                HospitalId.of(entity.getId()),
                entity.getName(),
                NodeId.of(entity.getNodeId()),
                entity.isEnabled(),
                entity.getCapacityTotal(),
                entity.getReservedSlots(),
                entity.getOccupiedSlots()
        );
    }
}

package com.arequiparescue.emergency.infrastructure.persistence;

import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.emergency.domain.EmergencyState;
import com.arequiparescue.emergency.domain.EquipmentType;
import com.arequiparescue.emergency.domain.IncidentCategory;
import com.arequiparescue.emergency.domain.SimulatedPriority;
import com.arequiparescue.emergency.domain.TriageRuleVersionId;
import com.arequiparescue.routing.domain.NodeId;
import com.arequiparescue.shared.domain.LogicalTime;

import java.util.Objects;

/**
 * Mapeador explícito entre {@link Emergency} (dominio puro) y {@link EmergencyJpaEntity} (persistencia JPA)
 * preservando todas las invariantes del dominio (DEC-003, ADR-0001 Secc. 4.3.1).
 */
public final class EmergencyPersistenceMapper {

    private EmergencyPersistenceMapper() {
    }

    public static EmergencyJpaEntity toJpaEntity(Emergency emergency) {
        Objects.requireNonNull(emergency, "La entidad de dominio Emergency a mapear no puede ser nula.");
        return new EmergencyJpaEntity(
                emergency.id().value(),
                emergency.category().code(),
                emergency.locationNode().value(),
                emergency.description(),
                emergency.simulatedPriority().name(),
                emergency.requiredEquipment().name(),
                emergency.appliedRuleVersionId().value(),
                emergency.state().name(),
                emergency.assignedAmbulanceId(),
                emergency.reservedOrOccupiedHospitalId(),
                emergency.registeredAt().ticks(),
                toNullableTicks(emergency.queueEnteredAt()),
                toNullableTicks(emergency.assignedAt()),
                toNullableTicks(emergency.atIncidentAt()),
                toNullableTicks(emergency.hospitalQueueEnteredAt()),
                toNullableTicks(emergency.inTransportAt()),
                toNullableTicks(emergency.completedAt()),
                emergency.lastOutcomeReason()
        );
    }

    public static Emergency toDomain(EmergencyJpaEntity entity) {
        Objects.requireNonNull(entity, "La entidad JPA EmergencyJpaEntity a mapear no puede ser nula.");
        return Emergency.reconstruct(
                EmergencyId.of(entity.getId()),
                IncidentCategory.fromCode(entity.getIncidentCategory()),
                NodeId.of(entity.getNodeId()),
                entity.getDescription(),
                SimulatedPriority.valueOf(entity.getSimulatedPriority()),
                EquipmentType.valueOf(entity.getRequiredEquipment()),
                TriageRuleVersionId.of(entity.getRuleVersionId()),
                EmergencyState.valueOf(entity.getState()),
                entity.getAssignedAmbulanceId(),
                entity.getHospitalId(),
                LogicalTime.of(entity.getRegisteredAtTicks()),
                toNullableLogicalTime(entity.getQueueEnteredAtTicks()),
                toNullableLogicalTime(entity.getAssignedAtTicks()),
                toNullableLogicalTime(entity.getAtIncidentAtTicks()),
                toNullableLogicalTime(entity.getHospitalQueueEnteredAtTicks()),
                toNullableLogicalTime(entity.getInTransportAtTicks()),
                toNullableLogicalTime(entity.getCompletedAtTicks()),
                entity.getLastOutcomeReason()
        );
    }

    private static Long toNullableTicks(LogicalTime time) {
        return time == null ? null : time.ticks();
    }

    private static LogicalTime toNullableLogicalTime(Long ticks) {
        return ticks == null ? null : LogicalTime.of(ticks);
    }
}

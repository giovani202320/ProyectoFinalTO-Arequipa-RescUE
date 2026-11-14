package com.arequiparescue.emergency.infrastructure.persistence;

import com.arequiparescue.emergency.domain.EquipmentType;
import com.arequiparescue.emergency.domain.IncidentCategory;
import com.arequiparescue.emergency.domain.SimulatedPriority;
import com.arequiparescue.emergency.domain.TriageRuleEntry;
import com.arequiparescue.emergency.domain.TriageRuleStatus;
import com.arequiparescue.emergency.domain.TriageRuleVersion;
import com.arequiparescue.emergency.domain.TriageRuleVersionId;
import com.arequiparescue.shared.domain.LogicalTime;

import java.util.List;
import java.util.Objects;

/**
 * Mapeador explícito entre el modelo JPA de versiones/reglas de triage y el modelo de dominio
 * (DEC-003, ADR-0001 Secc. 4.3.1).
 */
public final class TriageRuleVersionPersistenceMapper {

    private TriageRuleVersionPersistenceMapper() {
    }

    public static TriageRuleVersion toDomain(
            TriageRuleVersionJpaEntity versionEntity,
            List<TriageRuleJpaEntity> ruleEntities
    ) {
        Objects.requireNonNull(versionEntity, "La entidad JPA de versión de reglas no puede ser nula.");
        Objects.requireNonNull(ruleEntities, "La lista de entidades JPA de reglas no puede ser nula.");

        List<TriageRuleEntry> entries = ruleEntities.stream()
                .map(TriageRuleVersionPersistenceMapper::toDomainEntry)
                .toList();

        return new TriageRuleVersion(
                TriageRuleVersionId.of(versionEntity.getId()),
                versionEntity.getVersionNumber(),
                TriageRuleStatus.valueOf(versionEntity.getStatus()),
                LogicalTime.of(versionEntity.getPublishedAtTicks()),
                entries
        );
    }

    private static TriageRuleEntry toDomainEntry(TriageRuleJpaEntity entity) {
        return new TriageRuleEntry(
                IncidentCategory.fromCode(entity.getIncidentCategory()),
                SimulatedPriority.valueOf(entity.getSimulatedPriority()),
                EquipmentType.valueOf(entity.getRequiredEquipment()),
                entity.isActive()
        );
    }
}

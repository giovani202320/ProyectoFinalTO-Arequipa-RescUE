package com.arequiparescue.emergency.infrastructure.persistence;

import com.arequiparescue.emergency.domain.TriageRuleEntry;
import com.arequiparescue.emergency.domain.TriageRuleStatus;
import com.arequiparescue.emergency.domain.TriageRuleVersion;
import com.arequiparescue.emergency.domain.TriageRuleVersionId;
import com.arequiparescue.emergency.domain.port.TriageRuleVersionRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptador JPA que implementa el puerto {@link TriageRuleVersionRepository} (DEC-002, DEC-003,
 * DEC-010, DEC-016, ADR-0001, ADR-0002, HU-04).
 */
@Repository
public class TriageRuleVersionJpaAdapter implements TriageRuleVersionRepository {

    public static final String TRIAGE_RULE_VERSION_SEQUENCE_NAME = "TRIAGE_RULE_VERSION";

    private final SpringDataTriageRuleVersionRepository versionRepository;
    private final SpringDataTriageRuleRepository ruleRepository;
    private final SpringDataEmergencySequenceRepository sequenceRepository;

    public TriageRuleVersionJpaAdapter(
            SpringDataTriageRuleVersionRepository versionRepository,
            SpringDataTriageRuleRepository ruleRepository,
            SpringDataEmergencySequenceRepository sequenceRepository
    ) {
        this.versionRepository = versionRepository;
        this.ruleRepository = ruleRepository;
        this.sequenceRepository = sequenceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TriageRuleVersion> findActiveVersion() {
        return versionRepository
                .findFirstByStatusOrderByVersionNumberDesc(TriageRuleStatus.ACTIVE.name())
                .map(this::mapVersionWithRules);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TriageRuleVersion> findById(TriageRuleVersionId id) {
        Objects.requireNonNull(id, "El identificador de la versión de reglas no puede ser nulo.");
        return versionRepository.findById(id.value()).map(this::mapVersionWithRules);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TriageRuleVersion> findAllOrderByVersionNumberDesc() {
        return versionRepository.findAllByOrderByVersionNumberDesc()
                .stream()
                .map(this::mapVersionWithRules)
                .toList();
    }

    @Override
    @Transactional
    public int nextVersionNumber() {
        EmergencySequenceJpaEntity sequence = sequenceRepository
                .findByNameForUpdate(TRIAGE_RULE_VERSION_SEQUENCE_NAME)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró la secuencia '" + TRIAGE_RULE_VERSION_SEQUENCE_NAME + "' en emergency_sequences."));
        long current = sequence.getNextVal();
        sequence.setNextVal(Math.addExact(current, 1L));
        sequenceRepository.saveAndFlush(sequence);
        return Math.toIntExact(current);
    }

    @Override
    @Transactional
    public TriageRuleVersion publishAndActivate(TriageRuleVersion newActiveVersion) {
        Objects.requireNonNull(newActiveVersion, "La nueva versión de reglas a publicar y activar no puede ser nula.");

        List<TriageRuleVersionJpaEntity> previouslyActive =
                versionRepository.findByStatusForUpdate(TriageRuleStatus.ACTIVE.name());
        for (TriageRuleVersionJpaEntity activeEntity : previouslyActive) {
            activeEntity.setStatus(TriageRuleStatus.PUBLISHED.name());
            versionRepository.saveAndFlush(activeEntity);
        }

        TriageRuleVersionJpaEntity versionEntity = new TriageRuleVersionJpaEntity(
                newActiveVersion.id().value(),
                newActiveVersion.versionNumber(),
                TriageRuleStatus.ACTIVE.name(),
                newActiveVersion.publishedAt().ticks()
        );
        versionRepository.saveAndFlush(versionEntity);

        List<TriageRuleJpaEntity> ruleEntities = newActiveVersion.rules()
                .stream()
                .map(entry -> toRuleJpaEntity(newActiveVersion.id().value(), entry))
                .toList();
        ruleRepository.saveAllAndFlush(ruleEntities);

        return mapVersionWithRules(versionEntity);
    }

    private TriageRuleVersion mapVersionWithRules(TriageRuleVersionJpaEntity versionEntity) {
        List<TriageRuleJpaEntity> rules =
                ruleRepository.findByRuleVersionIdOrderByIncidentCategoryAsc(versionEntity.getId());
        return TriageRuleVersionPersistenceMapper.toDomain(versionEntity, rules);
    }

    private static TriageRuleJpaEntity toRuleJpaEntity(String versionId, TriageRuleEntry entry) {
        String ruleId = versionId + "-" + entry.category().code();
        return new TriageRuleJpaEntity(
                ruleId,
                versionId,
                entry.category().code(),
                entry.simulatedPriority().name(),
                entry.requiredEquipment().name(),
                entry.active()
        );
    }
}

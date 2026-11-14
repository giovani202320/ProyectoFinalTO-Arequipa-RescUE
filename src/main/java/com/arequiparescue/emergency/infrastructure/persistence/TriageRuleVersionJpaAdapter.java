package com.arequiparescue.emergency.infrastructure.persistence;

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
 * Adaptador JPA que implementa el puerto {@link TriageRuleVersionRepository} (DEC-002, DEC-003, ADR-0001).
 */
@Repository
public class TriageRuleVersionJpaAdapter implements TriageRuleVersionRepository {

    private final SpringDataTriageRuleVersionRepository versionRepository;
    private final SpringDataTriageRuleRepository ruleRepository;

    public TriageRuleVersionJpaAdapter(
            SpringDataTriageRuleVersionRepository versionRepository,
            SpringDataTriageRuleRepository ruleRepository
    ) {
        this.versionRepository = versionRepository;
        this.ruleRepository = ruleRepository;
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

    private TriageRuleVersion mapVersionWithRules(TriageRuleVersionJpaEntity versionEntity) {
        List<TriageRuleJpaEntity> rules =
                ruleRepository.findByRuleVersionIdOrderByIncidentCategoryAsc(versionEntity.getId());
        return TriageRuleVersionPersistenceMapper.toDomain(versionEntity, rules);
    }
}

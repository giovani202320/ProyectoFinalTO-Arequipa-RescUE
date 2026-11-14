package com.arequiparescue.emergency.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataTriageRuleRepository extends JpaRepository<TriageRuleJpaEntity, String> {

    List<TriageRuleJpaEntity> findByRuleVersionIdOrderByIncidentCategoryAsc(String ruleVersionId);
}

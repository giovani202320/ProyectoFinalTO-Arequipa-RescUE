package com.arequiparescue.emergency.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataTriageRuleVersionRepository extends JpaRepository<TriageRuleVersionJpaEntity, String> {

    Optional<TriageRuleVersionJpaEntity> findFirstByStatusOrderByVersionNumberDesc(String status);
}

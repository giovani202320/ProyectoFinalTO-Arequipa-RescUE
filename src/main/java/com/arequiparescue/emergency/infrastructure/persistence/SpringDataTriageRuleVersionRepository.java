package com.arequiparescue.emergency.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataTriageRuleVersionRepository extends JpaRepository<TriageRuleVersionJpaEntity, String> {

    Optional<TriageRuleVersionJpaEntity> findFirstByStatusOrderByVersionNumberDesc(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM TriageRuleVersionJpaEntity v WHERE v.status = :status ORDER BY v.versionNumber DESC")
    List<TriageRuleVersionJpaEntity> findByStatusForUpdate(@Param("status") String status);

    List<TriageRuleVersionJpaEntity> findAllByOrderByVersionNumberDesc();
}

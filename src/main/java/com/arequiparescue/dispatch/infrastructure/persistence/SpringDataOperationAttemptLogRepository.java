package com.arequiparescue.dispatch.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataOperationAttemptLogRepository extends JpaRepository<OperationAttemptLogJpaEntity, Long> {

    List<OperationAttemptLogJpaEntity> findByEmergencyIdOrderByIdAsc(String emergencyId);
}

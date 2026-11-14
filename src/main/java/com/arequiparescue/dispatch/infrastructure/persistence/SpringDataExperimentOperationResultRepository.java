package com.arequiparescue.dispatch.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataExperimentOperationResultRepository extends JpaRepository<ExperimentOperationResultJpaEntity, Long> {

    List<ExperimentOperationResultJpaEntity> findByEmergencyIdOrderByIdAsc(String emergencyId);

    List<ExperimentOperationResultJpaEntity> findTop10ByFinalStatusOrderByIdDesc(String finalStatus);
}

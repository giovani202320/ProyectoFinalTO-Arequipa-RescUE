package com.arequiparescue.simulation.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataSimulationScenarioRepository extends JpaRepository<SimulationScenarioJpaEntity, String> {

    boolean existsByStatus(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SimulationScenarioJpaEntity s WHERE s.status = :status ORDER BY s.id ASC")
    List<SimulationScenarioJpaEntity> findByStatusForUpdate(@Param("status") String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SimulationScenarioJpaEntity s WHERE s.id = :id")
    Optional<SimulationScenarioJpaEntity> findByIdForUpdate(@Param("id") String id);

    List<SimulationScenarioJpaEntity> findAllByOrderByIdAsc();

    Optional<SimulationScenarioJpaEntity> findFirstByStatusOrderByIdAsc(String status);
}

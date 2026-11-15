package com.arequiparescue.simulation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataSimulationEventRepository extends JpaRepository<SimulationEventJpaEntity, String> {

    @Query("""
            SELECT e FROM SimulationEventJpaEntity e
            WHERE e.scenarioId = :scenarioId
            ORDER BY e.scheduledAtTicks ASC, e.precedenceRank ASC, e.sequenceNumber ASC, e.id ASC
            """)
    List<SimulationEventJpaEntity> findByScenarioIdOrdered(@Param("scenarioId") String scenarioId);

    @Query("""
            SELECT e FROM SimulationEventJpaEntity e
            WHERE e.scenarioId = :scenarioId AND e.status = :status
            ORDER BY e.scheduledAtTicks ASC, e.precedenceRank ASC, e.sequenceNumber ASC, e.id ASC
            """)
    List<SimulationEventJpaEntity> findByScenarioIdAndStatusOrdered(
            @Param("scenarioId") String scenarioId,
            @Param("status") String status
    );

    Optional<SimulationEventJpaEntity> findFirstByScenarioIdAndStatusOrderByScheduledAtTicksAscPrecedenceRankAscSequenceNumberAscIdAsc(
            String scenarioId,
            String status
    );

    @Query("SELECT COALESCE(MAX(e.sequenceNumber), 0) FROM SimulationEventJpaEntity e WHERE e.scenarioId = :scenarioId")
    long findMaxSequenceNumberByScenarioId(@Param("scenarioId") String scenarioId);
}

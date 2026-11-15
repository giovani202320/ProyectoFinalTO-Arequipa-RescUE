package com.arequiparescue.simulation.domain.port;

import com.arequiparescue.simulation.domain.ScheduledSimulationEvent;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia para eventos discretos ordenados determinísticamente por escenario
 * ({@code DEC-019}, {@code RN-RUT-009}, {@code CA-SIM-07}).
 */
public interface SimulationEventRepository {

    ScheduledSimulationEvent save(ScheduledSimulationEvent event);

    Optional<ScheduledSimulationEvent> findById(String eventId);

    boolean existsById(String eventId);

    List<ScheduledSimulationEvent> findByScenarioIdOrdered(String scenarioId);

    List<ScheduledSimulationEvent> findPendingByScenarioIdOrdered(String scenarioId);

    Optional<ScheduledSimulationEvent> findNextPendingByScenarioId(String scenarioId);

    long nextSequenceNumber(String scenarioId);
}

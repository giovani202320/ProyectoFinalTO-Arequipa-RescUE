package com.arequiparescue.simulation.domain.port;

import com.arequiparescue.simulation.domain.SimulationScenario;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia para los escenarios experimentales de simulación (HU-13, ADR-0002).
 */
public interface SimulationScenarioRepository {

    SimulationScenario save(SimulationScenario scenario);

    Optional<SimulationScenario> findById(String scenarioId);

    /**
     * Carga el escenario adquiriendo bloqueo pesimista {@code SELECT ... FOR UPDATE}
     * como primer paso del orden canónico de ADR-0002.
     */
    Optional<SimulationScenario> findByIdForUpdate(String scenarioId);

    List<SimulationScenario> findAllOrderById();

    Optional<SimulationScenario> findActiveScenario();
}

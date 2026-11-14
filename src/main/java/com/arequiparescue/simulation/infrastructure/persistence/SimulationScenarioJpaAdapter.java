package com.arequiparescue.simulation.infrastructure.persistence;

import com.arequiparescue.simulation.domain.ScenarioStatus;
import com.arequiparescue.simulation.domain.port.ActiveScenarioChecker;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA que implementa {@link ActiveScenarioChecker} consultando {@code simulation_scenarios}
 * y respetando el orden canónico de bloqueo de ADR-0002 (Scenario primero).
 */
@Repository
public class SimulationScenarioJpaAdapter implements ActiveScenarioChecker {

    private final SpringDataSimulationScenarioRepository repository;

    public SimulationScenarioJpaAdapter(SpringDataSimulationScenarioRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAnyScenarioActive() {
        return repository.existsByStatus(ScenarioStatus.ACTIVE.name());
    }

    @Override
    @Transactional
    public boolean isAnyScenarioActiveForUpdate() {
        return !repository.findByStatusForUpdate(ScenarioStatus.ACTIVE.name()).isEmpty();
    }
}

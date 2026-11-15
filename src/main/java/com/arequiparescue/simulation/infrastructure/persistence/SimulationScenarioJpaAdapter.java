package com.arequiparescue.simulation.infrastructure.persistence;

import com.arequiparescue.shared.domain.LogicalTime;
import com.arequiparescue.simulation.domain.ScenarioStatus;
import com.arequiparescue.simulation.domain.SimulationScenario;
import com.arequiparescue.simulation.domain.port.ActiveScenarioChecker;
import com.arequiparescue.simulation.domain.port.SimulationScenarioRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador JPA que implementa {@link ActiveScenarioChecker} y {@link SimulationScenarioRepository}
 * consultando {@code simulation_scenarios} y respetando el orden canónico de bloqueo de ADR-0002 (Scenario primero).
 */
@Repository
public class SimulationScenarioJpaAdapter implements ActiveScenarioChecker, SimulationScenarioRepository {

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

    @Override
    @Transactional
    public SimulationScenario save(SimulationScenario scenario) {
        SimulationScenarioJpaEntity entity = repository.findById(scenario.id())
                .map(existing -> {
                    existing.setStatus(scenario.status().name());
                    existing.setCurrentLogicalTicks(scenario.currentTime().ticks());
                    return existing;
                })
                .orElseGet(() -> new SimulationScenarioJpaEntity(
                        scenario.id(),
                        scenario.name(),
                        scenario.status().name(),
                        scenario.currentTime().ticks()
                ));
        return toDomain(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SimulationScenario> findById(String scenarioId) {
        return repository.findById(scenarioId).map(SimulationScenarioJpaAdapter::toDomain);
    }

    @Override
    @Transactional
    public Optional<SimulationScenario> findByIdForUpdate(String scenarioId) {
        return repository.findByIdForUpdate(scenarioId).map(SimulationScenarioJpaAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SimulationScenario> findAllOrderById() {
        return repository.findAllByOrderByIdAsc().stream()
                .map(SimulationScenarioJpaAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SimulationScenario> findActiveScenario() {
        return repository.findFirstByStatusOrderByIdAsc(ScenarioStatus.ACTIVE.name())
                .map(SimulationScenarioJpaAdapter::toDomain);
    }

    private static SimulationScenario toDomain(SimulationScenarioJpaEntity entity) {
        return new SimulationScenario(
                entity.getId(),
                entity.getName(),
                ScenarioStatus.valueOf(entity.getStatus()),
                LogicalTime.of(entity.getCurrentLogicalTicks())
        );
    }
}

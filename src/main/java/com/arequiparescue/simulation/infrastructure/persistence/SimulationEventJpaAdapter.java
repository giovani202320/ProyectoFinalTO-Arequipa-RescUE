package com.arequiparescue.simulation.infrastructure.persistence;

import com.arequiparescue.shared.domain.LogicalTime;
import com.arequiparescue.simulation.domain.DiscreteEvent;
import com.arequiparescue.simulation.domain.DiscreteEventStatus;
import com.arequiparescue.simulation.domain.DiscreteEventType;
import com.arequiparescue.simulation.domain.ScheduledSimulationEvent;
import com.arequiparescue.simulation.domain.port.SimulationEventRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador JPA para la persistencia y ordenamiento determinista de eventos discretos por escenario (HU-13).
 */
@Repository
public class SimulationEventJpaAdapter implements SimulationEventRepository {

    private final SpringDataSimulationEventRepository repository;

    public SimulationEventJpaAdapter(SpringDataSimulationEventRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public ScheduledSimulationEvent save(ScheduledSimulationEvent scheduledEvent) {
        DiscreteEvent event = scheduledEvent.event();
        SimulationEventJpaEntity entity = repository.findById(event.eventId())
                .map(existing -> {
                    existing.setStatus(scheduledEvent.status().name());
                    existing.setExecutedAtTicks(scheduledEvent.executedAt().map(LogicalTime::ticks).orElse(null));
                    existing.setExecutionNote(scheduledEvent.executionNote());
                    return existing;
                })
                .orElseGet(() -> new SimulationEventJpaEntity(
                        event.eventId(),
                        scheduledEvent.scenarioId(),
                        event.scheduledAt().ticks(),
                        event.type().name(),
                        event.type().precedenceRank(),
                        event.sequenceNumber(),
                        event.subjectId(),
                        event.description(),
                        scheduledEvent.status().name(),
                        scheduledEvent.executedAt().map(LogicalTime::ticks).orElse(null),
                        scheduledEvent.executionNote()
                ));
        return toDomain(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ScheduledSimulationEvent> findById(String eventId) {
        return repository.findById(eventId).map(SimulationEventJpaAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(String eventId) {
        return repository.existsById(eventId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduledSimulationEvent> findByScenarioIdOrdered(String scenarioId) {
        return repository.findByScenarioIdOrdered(scenarioId).stream()
                .map(SimulationEventJpaAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduledSimulationEvent> findPendingByScenarioIdOrdered(String scenarioId) {
        return repository.findByScenarioIdAndStatusOrdered(scenarioId, DiscreteEventStatus.PENDING.name()).stream()
                .map(SimulationEventJpaAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ScheduledSimulationEvent> findNextPendingByScenarioId(String scenarioId) {
        return repository
                .findFirstByScenarioIdAndStatusOrderByScheduledAtTicksAscPrecedenceRankAscSequenceNumberAscIdAsc(
                        scenarioId,
                        DiscreteEventStatus.PENDING.name()
                )
                .map(SimulationEventJpaAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public long nextSequenceNumber(String scenarioId) {
        return repository.findMaxSequenceNumberByScenarioId(scenarioId) + 1L;
    }

    private static ScheduledSimulationEvent toDomain(SimulationEventJpaEntity entity) {
        DiscreteEvent event = new DiscreteEvent(
                entity.getId(),
                LogicalTime.of(entity.getScheduledAtTicks()),
                DiscreteEventType.valueOf(entity.getEventType()),
                entity.getSequenceNumber(),
                entity.getSubjectId(),
                entity.getDescription()
        );
        LogicalTime executedAt = entity.getExecutedAtTicks() == null
                ? null
                : LogicalTime.of(entity.getExecutedAtTicks());
        return new ScheduledSimulationEvent(
                entity.getScenarioId(),
                event,
                DiscreteEventStatus.valueOf(entity.getStatus()),
                executedAt,
                entity.getExecutionNote()
        );
    }
}

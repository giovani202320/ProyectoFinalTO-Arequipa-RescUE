package com.arequiparescue.simulation.application;

import com.arequiparescue.shared.domain.LogicalTime;
import com.arequiparescue.simulation.domain.ScheduledSimulationEvent;
import com.arequiparescue.simulation.domain.SimulationScenario;
import com.arequiparescue.simulation.domain.port.SimulationEventRepository;
import com.arequiparescue.simulation.domain.port.SimulationScenarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Servicio transaccional breve que reclama de forma atómica el siguiente evento pendiente de un
 * escenario bajo bloqueo pesimista {@code SELECT ... FOR UPDATE} (ADR-0002) y avanza monotónicamente
 * el tiempo lógico persistido antes de liberar el bloqueo para ejecutar efectos de aplicación.
 */
@Service
public class SimulationStepClaimService {

    private final SimulationScenarioRepository scenarioRepository;
    private final SimulationEventRepository eventRepository;

    public SimulationStepClaimService(
            SimulationScenarioRepository scenarioRepository,
            SimulationEventRepository eventRepository
    ) {
        this.scenarioRepository = Objects.requireNonNull(scenarioRepository);
        this.eventRepository = Objects.requireNonNull(eventRepository);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ClaimedStep claimNextPendingEvent(String scenarioId) {
        SimulationScenario scenario = scenarioRepository.findByIdForUpdate(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe el escenario experimental con identificador: " + scenarioId));
        if (!scenario.isActive()) {
            throw new IllegalStateException(
                    "El escenario " + scenario.id() + " no está activo (estado: " + scenario.status() + ").");
        }

        LogicalTime previousTime = scenario.currentTime();
        ScheduledSimulationEvent nextPending = eventRepository.findNextPendingByScenarioId(scenario.id())
                .orElse(null);
        if (nextPending == null) {
            return new ClaimedStep(scenario.id(), previousTime, previousTime, null);
        }

        LogicalTime targetTime = scenario.advanceClockTo(nextPending.event().scheduledAt());
        scenarioRepository.save(scenario);

        nextPending.markExecuted(
                targetTime,
                "Evento reclamado y ejecutado en t=" + targetTime.ticks() + "."
        );
        ScheduledSimulationEvent savedEvent = eventRepository.save(nextPending);
        return new ClaimedStep(scenario.id(), previousTime, targetTime, savedEvent);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ScheduledSimulationEvent markEventExecutionResult(String eventId, boolean failed, String note) {
        ScheduledSimulationEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el evento discreto: " + eventId));
        LogicalTime executionTime = event.executedAt().orElse(event.event().scheduledAt());
        if (failed) {
            event.markFailed(executionTime, note);
        } else {
            return eventRepository.save(new ScheduledSimulationEvent(
                    event.scenarioId(),
                    event.event(),
                    event.status(),
                    executionTime,
                    note
            ));
        }
        return eventRepository.save(event);
    }

    public record ClaimedStep(
            String scenarioId,
            LogicalTime previousTime,
            LogicalTime currentTime,
            ScheduledSimulationEvent claimedEvent
    ) {
    }
}

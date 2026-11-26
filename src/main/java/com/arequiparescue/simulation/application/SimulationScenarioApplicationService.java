package com.arequiparescue.simulation.application;

import com.arequiparescue.dispatch.application.WaitingQueueEvaluationResult;
import com.arequiparescue.shared.domain.LogicalClock;
import com.arequiparescue.shared.domain.LogicalTime;
import com.arequiparescue.simulation.domain.DiscreteEvent;
import com.arequiparescue.simulation.domain.ScheduledSimulationEvent;
import com.arequiparescue.simulation.domain.SimulationScenario;
import com.arequiparescue.simulation.domain.port.ActiveScenarioChecker;
import com.arequiparescue.simulation.domain.port.SimulationEventRepository;
import com.arequiparescue.simulation.domain.port.SimulationScenarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Servicio de aplicación para el ciclo de vida de escenarios experimentales y el avance manual
 * paso a paso del reloj lógico mediante eventos discretos (HU-13, RN-RUT-009, CA-SIM-06, CA-SIM-07).
 */
@Service
public class SimulationScenarioApplicationService {

    private final SimulationScenarioRepository scenarioRepository;
    private final SimulationEventRepository eventRepository;
    private final ActiveScenarioChecker activeScenarioChecker;
    private final SimulationStepClaimService stepClaimService;
    private final WaitingQueueSimulationTrigger simulationTrigger;
    private final LogicalClock logicalClock;

    public SimulationScenarioApplicationService(
            SimulationScenarioRepository scenarioRepository,
            SimulationEventRepository eventRepository,
            ActiveScenarioChecker activeScenarioChecker,
            SimulationStepClaimService stepClaimService,
            WaitingQueueSimulationTrigger simulationTrigger,
            LogicalClock logicalClock
    ) {
        this.scenarioRepository = Objects.requireNonNull(scenarioRepository);
        this.eventRepository = Objects.requireNonNull(eventRepository);
        this.activeScenarioChecker = Objects.requireNonNull(activeScenarioChecker);
        this.stepClaimService = Objects.requireNonNull(stepClaimService);
        this.simulationTrigger = Objects.requireNonNull(simulationTrigger);
        this.logicalClock = Objects.requireNonNull(logicalClock);
    }

    @Transactional
    public SimulationScenario createScenario(String scenarioId, String name) {
        if (scenarioId == null || scenarioId.isBlank()) {
            throw new IllegalArgumentException("El identificador del escenario es obligatorio.");
        }
        String normalizedId = scenarioId.trim();
        if (activeScenarioChecker.isAnyScenarioActiveForUpdate()) {
            throw new IllegalStateException(
                    "Ya existe un escenario experimental ACTIVE. Finalícelo antes de iniciar otro.");
        }
        if (scenarioRepository.findByIdForUpdate(normalizedId).isPresent()) {
            throw new IllegalArgumentException(
                    "Ya existe un escenario registrado con el identificador: " + normalizedId);
        }
        SimulationScenario created = SimulationScenario.startNew(normalizedId, name);
        return scenarioRepository.save(created);
    }

    @Transactional
    public ScheduledSimulationEvent scheduleEvent(ScheduleSimulationEventCommand command) {
        Objects.requireNonNull(command, "El comando para programar evento no puede ser nulo.");
        if (command.scenarioId() == null || command.scenarioId().isBlank()) {
            throw new IllegalArgumentException("El identificador del escenario es obligatorio.");
        }
        if (command.eventId() == null || command.eventId().isBlank()) {
            throw new IllegalArgumentException("El identificador del evento discreto es obligatorio.");
        }
        String scenarioId = command.scenarioId().trim();
        String eventId = command.eventId().trim();
        if (eventId.length() > 64) {
            throw new IllegalArgumentException("El identificador del evento no puede superar 64 caracteres.");
        }

        SimulationScenario scenario = scenarioRepository.findByIdForUpdate(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe el escenario experimental con identificador: " + scenarioId));

        LogicalTime scheduledAt = LogicalTime.of(command.scheduledAtTicks());
        scenario.validateCanScheduleAt(scheduledAt);

        if (eventRepository.existsById(eventId)) {
            throw new IllegalArgumentException(
                    "Ya existe un evento discreto registrado con el identificador: " + eventId);
        }

        long sequenceNumber = eventRepository.nextSequenceNumber(scenario.id());
        DiscreteEvent discreteEvent = new DiscreteEvent(
                eventId,
                scheduledAt,
                command.eventType(),
                sequenceNumber,
                command.subjectId() == null ? "" : command.subjectId().trim(),
                command.description() == null ? "" : command.description().trim()
        );
        return eventRepository.save(ScheduledSimulationEvent.pending(scenario.id(), discreteEvent));
    }

    /**
     * Avanza manualmente el reloj lógico al siguiente evento pendiente del escenario según el orden
     * determinista aprobado en DEC-019 (t ASC, precedencia de tipo ASC, secuencia ASC, id ASC).
     */
    public SimulationStepResult advanceNextStep(String scenarioId) {
        if (scenarioId == null || scenarioId.isBlank()) {
            throw new IllegalArgumentException("El identificador del escenario es obligatorio.");
        }
        SimulationStepClaimService.ClaimedStep claimed = stepClaimService.claimNextPendingEvent(scenarioId.trim());
        if (claimed.claimedEvent() == null) {
            return new SimulationStepResult(
                    claimed.scenarioId(),
                    SimulationStepOutcome.NO_PENDING_EVENTS,
                    claimed.previousTime(),
                    claimed.currentTime(),
                    null,
                    null,
                    "No hay eventos discretos pendientes por ejecutar en el escenario "
                            + claimed.scenarioId() + ". El reloj lógico permanece en t="
                            + claimed.currentTime().ticks() + "."
            );
        }

        synchronizeLogicalClockMonotonically(claimed.currentTime());

        ScheduledSimulationEvent claimedEvent = claimed.claimedEvent();
        try {
            Optional<WaitingQueueEvaluationResult> queueResult =
                    simulationTrigger.handleEvent(claimedEvent.event());
            String note = buildSuccessNote(claimedEvent, claimed.currentTime(), queueResult.orElse(null));
            ScheduledSimulationEvent updated = stepClaimService.markEventExecutionResult(
                    claimedEvent.event().eventId(),
                    false,
                    note
            );
            return new SimulationStepResult(
                    claimed.scenarioId(),
                    SimulationStepOutcome.EXECUTED,
                    claimed.previousTime(),
                    claimed.currentTime(),
                    updated,
                    queueResult.orElse(null),
                    note
            );
        } catch (RuntimeException ex) {
            String failureNote = "Error al ejecutar evento " + claimedEvent.event().eventId()
                    + " en t=" + claimed.currentTime().ticks() + ": " + ex.getMessage();
            ScheduledSimulationEvent failedEvent = stepClaimService.markEventExecutionResult(
                    claimedEvent.event().eventId(),
                    true,
                    failureNote
            );
            return new SimulationStepResult(
                    claimed.scenarioId(),
                    SimulationStepOutcome.FAILED,
                    claimed.previousTime(),
                    claimed.currentTime(),
                    failedEvent,
                    null,
                    failureNote
            );
        }
    }

    @Transactional
    public SimulationScenario completeScenario(String scenarioId) {
        if (scenarioId == null || scenarioId.isBlank()) {
            throw new IllegalArgumentException("El identificador del escenario es obligatorio.");
        }
        SimulationScenario scenario = scenarioRepository.findByIdForUpdate(scenarioId.trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe el escenario experimental con identificador: " + scenarioId));
        scenario.complete();
        return scenarioRepository.save(scenario);
    }

    @Transactional(readOnly = true)
    public List<SimulationScenario> listScenarios() {
        return scenarioRepository.findAllOrderById();
    }

    @Transactional(readOnly = true)
    public Optional<SimulationScenario> findScenario(String scenarioId) {
        return scenarioRepository.findById(scenarioId);
    }

    @Transactional(readOnly = true)
    public Optional<SimulationScenario> findActiveScenario() {
        return scenarioRepository.findActiveScenario();
    }

    @Transactional(readOnly = true)
    public List<ScheduledSimulationEvent> listScenarioEvents(String scenarioId) {
        return eventRepository.findByScenarioIdOrdered(scenarioId);
    }

    @Transactional(readOnly = true)
    public List<ScheduledSimulationEvent> listPendingEvents(String scenarioId) {
        return eventRepository.findPendingByScenarioIdOrdered(scenarioId);
    }

    @Transactional(readOnly = true)
    public Optional<ScheduledSimulationEvent> findNextPendingEvent(String scenarioId) {
        return eventRepository.findNextPendingByScenarioId(scenarioId);
    }

    private void synchronizeLogicalClockMonotonically(LogicalTime targetTime) {
        logicalClock.synchronizeAtLeast(targetTime);
    }

    private static String buildSuccessNote(
            ScheduledSimulationEvent event,
            LogicalTime executedAt,
            WaitingQueueEvaluationResult queueResult
    ) {
        String base = "Evento " + event.event().eventId()
                + " (" + event.event().type() + ", sujeto " + event.event().subjectId()
                + ") ejecutado en t=" + executedAt.ticks() + ".";
        if (queueResult == null) {
            return base;
        }
        return base + " Evaluación de cola: asignadas=" + queueResult.assignedCount()
                + ", en espera=" + queueResult.remainingWaitingCount() + ".";
    }
}

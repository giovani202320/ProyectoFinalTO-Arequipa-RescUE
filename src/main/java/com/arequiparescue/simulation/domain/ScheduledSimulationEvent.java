package com.arequiparescue.simulation.domain;

import com.arequiparescue.shared.domain.LogicalTime;

import java.util.Objects;
import java.util.Optional;

/**
 * Evento discreto persistido asociado a un escenario experimental, con su estado de ejecución
 * y trazabilidad de resultado (HU-13, RN-RUT-009, CA-SIM-06, CA-SIM-07).
 */
public final class ScheduledSimulationEvent {

    private final String scenarioId;
    private final DiscreteEvent event;
    private DiscreteEventStatus status;
    private LogicalTime executedAt;
    private String executionNote;

    public ScheduledSimulationEvent(
            String scenarioId,
            DiscreteEvent event,
            DiscreteEventStatus status,
            LogicalTime executedAt,
            String executionNote
    ) {
        Objects.requireNonNull(scenarioId, "El identificador del escenario no puede ser nulo.");
        if (scenarioId.isBlank()) {
            throw new IllegalArgumentException("El identificador del escenario no puede estar vacío.");
        }
        this.scenarioId = scenarioId.trim();
        this.event = Objects.requireNonNull(event, "El evento discreto no puede ser nulo.");
        this.status = Objects.requireNonNull(status, "El estado del evento no puede ser nulo.");
        if (executedAt != null && executedAt.isBefore(event.scheduledAt())) {
            throw new IllegalArgumentException(
                    "El instante de ejecución (" + executedAt + ") no puede ser anterior al programado ("
                            + event.scheduledAt() + ").");
        }
        this.executedAt = executedAt;
        this.executionNote = executionNote == null ? "" : executionNote;
    }

    public static ScheduledSimulationEvent pending(String scenarioId, DiscreteEvent event) {
        return new ScheduledSimulationEvent(scenarioId, event, DiscreteEventStatus.PENDING, null, "");
    }

    public String scenarioId() {
        return scenarioId;
    }

    public DiscreteEvent event() {
        return event;
    }

    public DiscreteEventStatus status() {
        return status;
    }

    public Optional<LogicalTime> executedAt() {
        return Optional.ofNullable(executedAt);
    }

    public String executionNote() {
        return executionNote;
    }

    public boolean isPending() {
        return status == DiscreteEventStatus.PENDING;
    }

    public void markExecuted(LogicalTime executionTime, String note) {
        if (!isPending()) {
            throw new IllegalStateException(
                    "Solo se puede ejecutar un evento en estado PENDING. Estado actual: " + status + ".");
        }
        Objects.requireNonNull(executionTime, "El tiempo de ejecución no puede ser nulo.");
        if (executionTime.isBefore(event.scheduledAt())) {
            throw new IllegalArgumentException(
                    "El tiempo de ejecución no puede ser anterior al instante programado del evento.");
        }
        this.status = DiscreteEventStatus.EXECUTED;
        this.executedAt = executionTime;
        this.executionNote = truncateNote(note);
    }

    public void markFailed(LogicalTime attemptTime, String note) {
        if (status == DiscreteEventStatus.FAILED) {
            return;
        }
        Objects.requireNonNull(attemptTime, "El tiempo de intento no puede ser nulo.");
        if (attemptTime.isBefore(event.scheduledAt())) {
            throw new IllegalArgumentException(
                    "El tiempo de fallo no puede ser anterior al instante programado del evento.");
        }
        this.status = DiscreteEventStatus.FAILED;
        this.executedAt = attemptTime;
        this.executionNote = truncateNote(note);
    }

    private static String truncateNote(String note) {
        if (note == null) {
            return "";
        }
        return note.length() <= 500 ? note : note.substring(0, 500);
    }
}

package com.arequiparescue.simulation.domain;

import com.arequiparescue.shared.domain.LogicalClock;
import com.arequiparescue.shared.domain.LogicalTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * Planificador básico de eventos discretos con avance manual y reloj lógico monotónico (t >= 0).
 * Garantiza que no se agenden eventos en el pasado respecto al reloj actual y que los eventos
 * simultáneos se despachen en orden estrictamente determinista (DEC-019, RN-RUT-009, CA-SIM-06, CA-SIM-07).
 */
public final class DiscreteEventScheduler {

    private final LogicalClock clock;
    private final PriorityQueue<DiscreteEvent> pendingQueue;
    private long nextSequenceNumber;

    public DiscreteEventScheduler() {
        this(new LogicalClock());
    }

    public DiscreteEventScheduler(LogicalClock clock) {
        this.clock = Objects.requireNonNull(clock, "El reloj lógico no puede ser nulo.");
        this.pendingQueue = new PriorityQueue<>();
        this.nextSequenceNumber = 1L;
    }

    public LogicalClock clock() {
        return clock;
    }

    public LogicalTime currentTime() {
        return clock.currentTime();
    }

    /**
     * Registra un nuevo evento asignándole la siguiente secuencia monotónica de creación.
     * Rechaza cualquier intento de agendar un evento en un instante anterior al tiempo lógico actual.
     */
    public DiscreteEvent schedule(
            String eventId,
            LogicalTime scheduledAt,
            DiscreteEventType type,
            String subjectId,
            String description
    ) {
        long assignedSequence = nextSequenceNumber++;
        DiscreteEvent event = new DiscreteEvent(
                eventId,
                scheduledAt,
                type,
                assignedSequence,
                subjectId,
                description
        );
        return register(event);
    }

    /**
     * Registra un evento discreto ya construido validando que su tiempo lógico no sea anterior
     * al tiempo actual del reloj y ajustando el generador de secuencia monotónica.
     */
    public DiscreteEvent register(DiscreteEvent event) {
        Objects.requireNonNull(event, "El evento a registrar no puede ser nulo.");
        if (event.scheduledAt().isBefore(clock.currentTime())) {
            throw new IllegalArgumentException(
                    "No se permite registrar un evento en el pasado lógico: evento en "
                            + event.scheduledAt() + " con reloj actual en " + clock.currentTime() + ".");
        }
        if (event.sequenceNumber() >= nextSequenceNumber) {
            nextSequenceNumber = event.sequenceNumber() + 1L;
        }
        pendingQueue.add(event);
        return event;
    }

    public Optional<DiscreteEvent> peekNext() {
        return Optional.ofNullable(pendingQueue.peek());
    }

    /**
     * Extrae el siguiente evento pendiente según el orden determinista aprobado y avanza
     * monotónicamente el reloj lógico hasta el instante programado del evento.
     */
    public Optional<DiscreteEvent> stepNext() {
        DiscreteEvent next = pendingQueue.poll();
        if (next == null) {
            return Optional.empty();
        }
        clock.advanceTo(next.scheduledAt());
        return Optional.of(next);
    }

    public int pendingCount() {
        return pendingQueue.size();
    }

    public boolean hasPendingEvents() {
        return !pendingQueue.isEmpty();
    }

    /**
     * Devuelve una vista inmutable de los eventos pendientes en el orden determinista exacto
     * en que serían procesados, sin alterar la cola ni avanzar el reloj.
     */
    public List<DiscreteEvent> snapshotOrderedPendingEvents() {
        List<DiscreteEvent> copy = new ArrayList<>(pendingQueue);
        Collections.sort(copy);
        return List.copyOf(copy);
    }
}

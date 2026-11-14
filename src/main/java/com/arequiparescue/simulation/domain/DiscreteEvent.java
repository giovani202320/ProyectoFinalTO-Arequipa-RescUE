package com.arequiparescue.simulation.domain;

import com.arequiparescue.shared.domain.LogicalTime;

import java.util.Comparator;
import java.util.Objects;

/**
 * Evento discreto agendado en el motor de simulación de ARE.
 * Implementa el ordenamiento determinista aprobado en DEC-019 (RN-RUT-009, CA-SIM-07):
 * 1. Menor tiempo lógico programado (scheduledAt ASC).
 * 2. Orden fijo de precedencia por tipo de evento (type.precedenceRank() ASC: 1 -> 2 -> 3 -> 4).
 * 3. Secuencia monotónica creciente de creación (sequenceNumber ASC).
 * 4. Identificador técnico estable en orden lexicográfico ascendente (eventId ASC).
 */
public record DiscreteEvent(
        String eventId,
        LogicalTime scheduledAt,
        DiscreteEventType type,
        long sequenceNumber,
        String subjectId,
        String description
) implements Comparable<DiscreteEvent> {

    private static final Comparator<DiscreteEvent> DETERMINISTIC_COMPARATOR = Comparator
            .comparing(DiscreteEvent::scheduledAt)
            .thenComparingInt(event -> event.type().precedenceRank())
            .thenComparingLong(DiscreteEvent::sequenceNumber)
            .thenComparing(DiscreteEvent::eventId);

    public DiscreteEvent {
        Objects.requireNonNull(eventId, "El identificador del evento no puede ser nulo.");
        if (eventId.isBlank()) {
            throw new IllegalArgumentException("El identificador del evento no puede estar vacío.");
        }
        Objects.requireNonNull(scheduledAt, "El instante lógico programado no puede ser nulo.");
        Objects.requireNonNull(type, "El tipo de evento discreto no puede ser nulo.");
        if (sequenceNumber <= 0L) {
            throw new IllegalArgumentException(
                    "La secuencia monotónica de creación debe ser positiva (> 0). Valor recibido: " + sequenceNumber);
        }
        Objects.requireNonNull(subjectId, "El identificador del sujeto del evento no puede ser nulo.");
        if (subjectId.isBlank()) {
            throw new IllegalArgumentException("El identificador del sujeto del evento no puede estar vacío.");
        }
        description = description == null ? "" : description;
    }

    @Override
    public int compareTo(DiscreteEvent other) {
        Objects.requireNonNull(other, "El evento discreto a comparar no puede ser nulo.");
        return DETERMINISTIC_COMPARATOR.compare(this, other);
    }
}

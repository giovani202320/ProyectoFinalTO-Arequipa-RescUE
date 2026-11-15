package com.arequiparescue.simulation.domain;

import com.arequiparescue.shared.domain.LogicalTime;

import java.util.Objects;

/**
 * Agregado de dominio que representa un escenario experimental de simulación y su reloj lógico monotónico
 * ({@code t >= 0}) conforme a {@code RN-RUT-009}, {@code CA-SIM-06} y {@code HU-13}.
 */
public final class SimulationScenario {

    private final String id;
    private final String name;
    private ScenarioStatus status;
    private LogicalTime currentTime;

    public SimulationScenario(String id, String name, ScenarioStatus status, LogicalTime currentTime) {
        Objects.requireNonNull(id, "El identificador del escenario no puede ser nulo.");
        if (id.isBlank()) {
            throw new IllegalArgumentException("El identificador del escenario no puede estar vacío.");
        }
        if (id.length() > 32) {
            throw new IllegalArgumentException("El identificador del escenario no puede superar 32 caracteres.");
        }
        Objects.requireNonNull(name, "El nombre del escenario no puede ser nulo.");
        if (name.isBlank()) {
            throw new IllegalArgumentException("El nombre del escenario no puede estar vacío.");
        }
        if (name.length() > 120) {
            throw new IllegalArgumentException("El nombre del escenario no puede superar 120 caracteres.");
        }
        this.id = id.trim();
        this.name = name.trim();
        this.status = Objects.requireNonNull(status, "El estado del escenario no puede ser nulo.");
        this.currentTime = Objects.requireNonNull(currentTime, "El tiempo lógico actual no puede ser nulo.");
    }

    public static SimulationScenario startNew(String id, String name) {
        return new SimulationScenario(id, name, ScenarioStatus.ACTIVE, LogicalTime.ZERO);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public ScenarioStatus status() {
        return status;
    }

    public LogicalTime currentTime() {
        return currentTime;
    }

    public boolean isActive() {
        return status == ScenarioStatus.ACTIVE;
    }

    /**
     * Verifica si el escenario admite agendar un evento en el instante lógico indicado.
     */
    public void validateCanScheduleAt(LogicalTime scheduledAt) {
        Objects.requireNonNull(scheduledAt, "El instante lógico programado no puede ser nulo.");
        if (!isActive()) {
            throw new IllegalStateException(
                    "Solo se pueden programar eventos en un escenario ACTIVE. Estado actual: " + status + ".");
        }
        if (scheduledAt.isBefore(currentTime)) {
            throw new IllegalArgumentException(
                    "No se permite programar un evento en el pasado lógico: evento en "
                            + scheduledAt + " con reloj actual del escenario en " + currentTime + " (RN-RUT-009).");
        }
    }

    /**
     * Avanza monotónicamente el tiempo lógico del escenario hasta {@code targetTime}.
     */
    public LogicalTime advanceClockTo(LogicalTime targetTime) {
        Objects.requireNonNull(targetTime, "El instante lógico objetivo no puede ser nulo.");
        if (!isActive()) {
            throw new IllegalStateException(
                    "No se puede avanzar el reloj de un escenario en estado " + status + ".");
        }
        if (targetTime.isBefore(this.currentTime)) {
            throw new IllegalArgumentException(
                    "Violación de monotonicidad del reloj lógico: no se permite retroceder de "
                            + this.currentTime + " a " + targetTime + " (RN-RUT-009).");
        }
        this.currentTime = targetTime;
        return this.currentTime;
    }

    /**
     * Cierra el escenario experimental ({@code ACTIVE -> COMPLETED}).
     */
    public void complete() {
        if (!isActive()) {
            throw new IllegalStateException(
                    "El escenario " + id + " ya se encuentra en estado " + status + ".");
        }
        this.status = ScenarioStatus.COMPLETED;
    }
}

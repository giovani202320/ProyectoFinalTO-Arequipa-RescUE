package com.arequiparescue.simulation.application;

/**
 * Resultado del intento de avance manual paso a paso del reloj lógico (HU-13).
 */
public enum SimulationStepOutcome {
    EXECUTED,
    NO_PENDING_EVENTS,
    FAILED
}

package com.arequiparescue.simulation.domain;

/**
 * Estado de ejecución de un evento discreto persistido dentro de un escenario experimental (HU-13).
 */
public enum DiscreteEventStatus {
    PENDING,
    EXECUTED,
    FAILED
}

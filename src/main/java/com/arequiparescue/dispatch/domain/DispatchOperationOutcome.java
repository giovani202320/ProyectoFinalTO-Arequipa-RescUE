package com.arequiparescue.dispatch.domain;

/**
 * Resultado funcional de una operación de asignación supervisada de ambulancia (HU-06).
 */
public enum DispatchOperationOutcome {
    ASSIGNED,
    CONFLICT_RELOAD_REQUIRED,
    MOVED_TO_WAITING_AMBULANCE
}

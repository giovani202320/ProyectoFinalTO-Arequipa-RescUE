package com.arequiparescue.dispatch.domain;

/**
 * Resultado funcional de una operación de asignación supervisada (HU-06) o automática (HU-07).
 */
public enum DispatchOperationOutcome {
    ASSIGNED,
    CONFLICT_RELOAD_REQUIRED,
    MOVED_TO_WAITING_AMBULANCE,
    CONCURRENCY_RETRY_EXHAUSTED
}

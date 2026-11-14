package com.arequiparescue.dispatch.application;

import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.dispatch.domain.DispatchOperationOutcome;
import com.arequiparescue.emergency.domain.Emergency;

/**
 * Resultado de una operación de asignación automática de ambulancia (HU-07, DEC-017, RN-CON-008).
 */
public record AutomaticDispatchResult(
        String operationId,
        DispatchOperationOutcome outcome,
        Emergency emergency,
        Ambulance assignedAmbulance,
        int attemptsExecuted,
        int retriesConsumed,
        String outcomeCode,
        String message
) {
}

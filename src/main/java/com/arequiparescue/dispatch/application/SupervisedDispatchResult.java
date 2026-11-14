package com.arequiparescue.dispatch.application;

import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.dispatch.domain.DispatchOperationOutcome;
import com.arequiparescue.dispatch.domain.EligibleAmbulanceCandidate;
import com.arequiparescue.emergency.domain.Emergency;

import java.util.List;
import java.util.Objects;

/**
 * Resultado de la operación de asignación supervisada (HU-06).
 */
public record SupervisedDispatchResult(
        String operationId,
        DispatchOperationOutcome outcome,
        Emergency emergency,
        Ambulance assignedAmbulance,
        List<EligibleAmbulanceCandidate> refreshedCandidates,
        String message
) {

    public SupervisedDispatchResult {
        Objects.requireNonNull(operationId);
        Objects.requireNonNull(outcome);
        Objects.requireNonNull(emergency);
        refreshedCandidates = refreshedCandidates == null ? List.of() : List.copyOf(refreshedCandidates);
        Objects.requireNonNull(message);
    }
}

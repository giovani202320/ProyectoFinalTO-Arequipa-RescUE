package com.arequiparescue.dispatch.domain.port;

/**
 * Puerto de dominio para persistir trazas independientes de intentos y resultados de despacho
 * (ADR-0002 Decisión 8, RN-CON-008).
 */
public interface DispatchTraceabilityRepository {

    void recordAttempt(
            String operationId,
            String operationType,
            String emergencyId,
            String candidateResourceId,
            int attemptNumber,
            String outcomeCode,
            String detailMessage,
            long logicalTicks
    );

    void recordOperationResult(
            String operationId,
            String operationType,
            String emergencyId,
            String assignedResourceId,
            int totalAttempts,
            String finalStatus,
            String outcomeReason,
            long logicalTicks
    );
}

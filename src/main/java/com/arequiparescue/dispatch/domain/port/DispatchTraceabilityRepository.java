package com.arequiparescue.dispatch.domain.port;

import java.util.List;

/**
 * Puerto de dominio para persistir y consultar trazas independientes de intentos y resultados de despacho
 * (ADR-0002 Decisión 8, RN-CON-008).
 */
public interface DispatchTraceabilityRepository {

    record ExhaustedDispatchNotice(
            String operationId,
            String operationType,
            String emergencyId,
            int totalAttempts,
            String finalStatus,
            String outcomeReason,
            long logicalTicks
    ) {
    }

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

    default List<ExhaustedDispatchNotice> findRecentConcurrencyExhaustedNotices() {
        return List.of();
    }

    default List<ExhaustedDispatchNotice> findRecentHospitalExhaustedNotices() {
        return List.of();
    }
}

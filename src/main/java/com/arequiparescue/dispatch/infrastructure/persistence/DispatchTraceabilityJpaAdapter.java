package com.arequiparescue.dispatch.infrastructure.persistence;

import com.arequiparescue.dispatch.domain.port.DispatchTraceabilityRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA que persiste trazas de intentos y resultados de despacho en transacciones
 * independientes {@code REQUIRES_NEW} para conservar la evidencia incluso cuando un intento
 * concurrente revierte su transacción (ADR-0002 Decisión 8, RN-CON-008).
 */
@Repository
public class DispatchTraceabilityJpaAdapter implements DispatchTraceabilityRepository {

    private final SpringDataOperationAttemptLogRepository attemptLogRepository;
    private final SpringDataExperimentOperationResultRepository operationResultRepository;

    public DispatchTraceabilityJpaAdapter(
            SpringDataOperationAttemptLogRepository attemptLogRepository,
            SpringDataExperimentOperationResultRepository operationResultRepository
    ) {
        this.attemptLogRepository = attemptLogRepository;
        this.operationResultRepository = operationResultRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAttempt(
            String operationId,
            String operationType,
            String emergencyId,
            String candidateResourceId,
            int attemptNumber,
            String outcomeCode,
            String detailMessage,
            long logicalTicks
    ) {
        attemptLogRepository.saveAndFlush(new OperationAttemptLogJpaEntity(
                operationId,
                operationType,
                emergencyId,
                candidateResourceId,
                attemptNumber,
                outcomeCode,
                truncate(detailMessage, 500),
                logicalTicks
        ));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordOperationResult(
            String operationId,
            String operationType,
            String emergencyId,
            String assignedResourceId,
            int totalAttempts,
            String finalStatus,
            String outcomeReason,
            long logicalTicks
    ) {
        operationResultRepository.saveAndFlush(new ExperimentOperationResultJpaEntity(
                operationId,
                operationType,
                emergencyId,
                assignedResourceId,
                totalAttempts,
                finalStatus,
                truncate(outcomeReason, 500),
                logicalTicks
        ));
    }

    private static String truncate(String value, int maxLen) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }
}

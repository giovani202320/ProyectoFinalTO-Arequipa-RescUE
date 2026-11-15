package com.arequiparescue.reporting.domain;

/**
 * Métrica 6 de {@code RN-MET-004} y {@code CA-SIM-09}: conteo exacto y desglosado de conflictos
 * concurrentes, reintentos consumidos y agotamientos entre asignación de ambulancias ({@code DEC-017})
 * y reserva hospitalaria ({@code DEC-018}).
 */
public record ConcurrencyMetric(
        OperationBreakdown ambulanceDispatch,
        OperationBreakdown hospitalReservation,
        long totalConflicts,
        long totalRetriesConsumed,
        long totalExhaustions
) {

    public record OperationBreakdown(
            long conflicts,
            long retriesConsumed,
            long exhaustions
    ) {
    }
}

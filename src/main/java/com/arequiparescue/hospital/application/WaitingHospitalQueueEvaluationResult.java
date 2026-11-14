package com.arequiparescue.hospital.application;

import java.util.List;

/**
 * Resultado consolidado de un ciclo de reevaluación FIFO de la cola {@code WAITING_HOSPITAL} (HU-10).
 */
public record WaitingHospitalQueueEvaluationResult(
        int evaluatedWaitingCount,
        int reservedCount,
        int remainingWaitingCount,
        List<HospitalReservationResult> results,
        String summaryMessage
) {
}

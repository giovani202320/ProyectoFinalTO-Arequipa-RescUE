package com.arequiparescue.hospital.application;

import com.arequiparescue.hospital.domain.Hospital;
import com.arequiparescue.hospital.domain.HospitalDischargeOutcome;
import com.arequiparescue.hospital.domain.HospitalReservation;

/**
 * Resultado de aplicación de un alta hospitalaria simulada (HU-12).
 */
public record HospitalDischargeResult(
        String operationId,
        HospitalDischargeOutcome outcome,
        Hospital hospital,
        HospitalReservation reservation,
        WaitingHospitalQueueEvaluationResult hospitalQueueEvaluation,
        String message
) {
}

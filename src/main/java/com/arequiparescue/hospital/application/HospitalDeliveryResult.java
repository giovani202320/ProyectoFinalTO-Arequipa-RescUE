package com.arequiparescue.hospital.application;

import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.dispatch.application.WaitingQueueEvaluationResult;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.hospital.domain.Hospital;
import com.arequiparescue.hospital.domain.HospitalDeliveryOutcome;
import com.arequiparescue.hospital.domain.HospitalReservation;

/**
 * Resultado de aplicación de una entrega hospitalaria simulada (HU-11).
 */
public record HospitalDeliveryResult(
        String operationId,
        HospitalDeliveryOutcome outcome,
        Emergency emergency,
        Ambulance releasedAmbulance,
        Hospital hospital,
        HospitalReservation reservation,
        WaitingQueueEvaluationResult ambulanceQueueEvaluation,
        String message
) {
}

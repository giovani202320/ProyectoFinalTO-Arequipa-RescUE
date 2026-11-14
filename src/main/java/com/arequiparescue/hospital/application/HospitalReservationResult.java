package com.arequiparescue.hospital.application;

import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.hospital.domain.Hospital;
import com.arequiparescue.hospital.domain.HospitalDispatchOutcome;

/**
 * Resultado de una operación de llegada al incidente y/o reserva hospitalaria (HU-09, HU-10).
 */
public record HospitalReservationResult(
        String operationId,
        HospitalDispatchOutcome outcome,
        Emergency emergency,
        Ambulance ambulance,
        Hospital reservedHospital,
        int attemptsExecuted,
        int retriesConsumed,
        String outcomeCode,
        String message
) {
}

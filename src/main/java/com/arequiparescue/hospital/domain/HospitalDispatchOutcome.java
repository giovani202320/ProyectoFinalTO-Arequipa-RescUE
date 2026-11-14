package com.arequiparescue.hospital.domain;

/**
 * Resultado funcional de una operación de llegada al incidente y/o reserva hospitalaria
 * (HU-09, HU-10, DEC-018, RN-HOS-007 a RN-HOS-009).
 */
public enum HospitalDispatchOutcome {
    IN_TRANSPORT,
    MOVED_TO_WAITING_HOSPITAL,
    HOSPITAL_RESERVATION_RETRY_EXHAUSTED,
    DUPLICATE_OR_INVALID_STATE
}

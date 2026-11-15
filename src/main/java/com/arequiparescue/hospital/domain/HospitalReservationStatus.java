package com.arequiparescue.hospital.domain;

/**
 * Estados del ciclo de vida de una reserva/ocupación hospitalaria (HU-09 a HU-12, Flyway V6).
 */
public enum HospitalReservationStatus {
    RESERVED,
    OCCUPIED,
    DISCHARGED
}

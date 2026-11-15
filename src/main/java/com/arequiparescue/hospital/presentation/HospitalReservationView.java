package com.arequiparescue.hospital.presentation;

import com.arequiparescue.hospital.domain.HospitalReservation;

/**
 * Modelo de presentación (DTO) de una reserva/ocupación hospitalaria (HU-09 a HU-12).
 */
public record HospitalReservationView(
        String id,
        String emergencyId,
        String hospitalId,
        String status,
        long reservedAtTicks,
        Long occupiedAtTicks,
        Long dischargedAtTicks
) {

    public static HospitalReservationView fromDomain(HospitalReservation reservation) {
        return new HospitalReservationView(
                reservation.id(),
                reservation.emergencyId().value(),
                reservation.hospitalId().value(),
                reservation.status().name(),
                reservation.reservedAt().ticks(),
                reservation.occupiedAt() != null ? reservation.occupiedAt().ticks() : null,
                reservation.dischargedAt() != null ? reservation.dischargedAt().ticks() : null
        );
    }
}

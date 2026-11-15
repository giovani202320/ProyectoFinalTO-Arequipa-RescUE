package com.arequiparescue.hospital.domain;

import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.shared.domain.LogicalTime;

import java.util.Objects;

/**
 * Entidad de dominio puro que representa una reserva y eventual ocupación hospitalaria única
 * por emergencia (HU-09 a HU-12, DEC-018, RN-HOS-012, RN-HOS-013).
 * Libre de dependencias de Spring, JPA, Hibernate o Servlet.
 */
public final class HospitalReservation {

    private final String id;
    private final EmergencyId emergencyId;
    private final HospitalId hospitalId;
    private final HospitalReservationStatus status;
    private final LogicalTime reservedAt;
    private final LogicalTime occupiedAt;
    private final LogicalTime dischargedAt;

    private HospitalReservation(
            String id,
            EmergencyId emergencyId,
            HospitalId hospitalId,
            HospitalReservationStatus status,
            LogicalTime reservedAt,
            LogicalTime occupiedAt,
            LogicalTime dischargedAt
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El identificador de la reserva hospitalaria es obligatorio.");
        }
        this.id = id.trim();
        this.emergencyId = Objects.requireNonNull(emergencyId, "El identificador de la emergencia es obligatorio.");
        this.hospitalId = Objects.requireNonNull(hospitalId, "El identificador del hospital es obligatorio.");
        this.status = Objects.requireNonNull(status, "El estado de la reserva hospitalaria es obligatorio.");
        this.reservedAt = Objects.requireNonNull(reservedAt, "El instante lógico de reserva es obligatorio.");
        this.occupiedAt = occupiedAt;
        this.dischargedAt = dischargedAt;
    }

    public static HospitalReservation reconstruct(
            String id,
            EmergencyId emergencyId,
            HospitalId hospitalId,
            HospitalReservationStatus status,
            LogicalTime reservedAt,
            LogicalTime occupiedAt,
            LogicalTime dischargedAt
    ) {
        return new HospitalReservation(id, emergencyId, hospitalId, status, reservedAt, occupiedAt, dischargedAt);
    }

    /**
     * HU-11 (RN-HOS-012, CA-HOS-10): Transforma una reserva {@code RESERVED} en ocupación {@code OCCUPIED}.
     */
    public HospitalReservation occupy(LogicalTime at) {
        Objects.requireNonNull(at, "El instante lógico de ocupación es obligatorio.");
        if (this.status != HospitalReservationStatus.RESERVED) {
            throw new IllegalStateException(
                    "Solo una reserva en estado RESERVED puede convertirse en OCCUPIED. Estado actual: " + this.status);
        }
        if (at.ticks() < this.reservedAt.ticks()) {
            throw new IllegalArgumentException(
                    "El instante de ocupación no puede preceder al instante de reserva.");
        }
        return new HospitalReservation(
                this.id,
                this.emergencyId,
                this.hospitalId,
                HospitalReservationStatus.OCCUPIED,
                this.reservedAt,
                at,
                null
        );
    }

    /**
     * HU-12 (RN-HOS-013, CA-HOS-11): Ejecuta el alta simulada sobre una ocupación {@code OCCUPIED -> DISCHARGED}.
     */
    public HospitalReservation discharge(LogicalTime at) {
        Objects.requireNonNull(at, "El instante lógico de alta simulada es obligatorio.");
        if (this.status != HospitalReservationStatus.OCCUPIED) {
            throw new IllegalStateException(
                    "Solo una ocupación en estado OCCUPIED puede recibir alta simulada. Estado actual: " + this.status);
        }
        if (this.occupiedAt != null && at.ticks() < this.occupiedAt.ticks()) {
            throw new IllegalArgumentException(
                    "El instante de alta simulada no puede preceder al instante de ocupación.");
        }
        return new HospitalReservation(
                this.id,
                this.emergencyId,
                this.hospitalId,
                HospitalReservationStatus.DISCHARGED,
                this.reservedAt,
                this.occupiedAt,
                at
        );
    }

    public String id() {
        return id;
    }

    public EmergencyId emergencyId() {
        return emergencyId;
    }

    public HospitalId hospitalId() {
        return hospitalId;
    }

    public HospitalReservationStatus status() {
        return status;
    }

    public LogicalTime reservedAt() {
        return reservedAt;
    }

    public LogicalTime occupiedAt() {
        return occupiedAt;
    }

    public LogicalTime dischargedAt() {
        return dischargedAt;
    }
}

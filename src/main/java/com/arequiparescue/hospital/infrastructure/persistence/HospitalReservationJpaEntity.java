package com.arequiparescue.hospital.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad JPA para la tabla {@code hospital_reservations} (Flyway V6, HU-09, HU-10).
 */
@Entity
@Table(name = "hospital_reservations")
public class HospitalReservationJpaEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "emergency_id", length = 32, nullable = false, unique = true)
    private String emergencyId;

    @Column(name = "hospital_id", length = 32, nullable = false)
    private String hospitalId;

    @Column(name = "status", length = 16, nullable = false)
    private String status;

    @Column(name = "reserved_at_t", nullable = false)
    private long reservedAtTicks;

    @Column(name = "occupied_at_t")
    private Long occupiedAtTicks;

    @Column(name = "discharged_at_t")
    private Long dischargedAtTicks;

    protected HospitalReservationJpaEntity() {
        // Requerido por JPA
    }

    public HospitalReservationJpaEntity(
            String id,
            String emergencyId,
            String hospitalId,
            String status,
            long reservedAtTicks,
            Long occupiedAtTicks,
            Long dischargedAtTicks
    ) {
        this.id = id;
        this.emergencyId = emergencyId;
        this.hospitalId = hospitalId;
        this.status = status;
        this.reservedAtTicks = reservedAtTicks;
        this.occupiedAtTicks = occupiedAtTicks;
        this.dischargedAtTicks = dischargedAtTicks;
    }

    public String getId() {
        return id;
    }

    public String getEmergencyId() {
        return emergencyId;
    }

    public String getHospitalId() {
        return hospitalId;
    }

    public String getStatus() {
        return status;
    }

    public long getReservedAtTicks() {
        return reservedAtTicks;
    }

    public Long getOccupiedAtTicks() {
        return occupiedAtTicks;
    }

    public Long getDischargedAtTicks() {
        return dischargedAtTicks;
    }
}

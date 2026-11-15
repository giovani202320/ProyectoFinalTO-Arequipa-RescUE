package com.arequiparescue.hospital.domain.port;

import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.hospital.domain.HospitalId;
import com.arequiparescue.hospital.domain.HospitalReservation;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de dominio para persistir y validar unicidad de reservas/ocupaciones hospitalarias
 * sin acoplar el dominio a JPA (HU-09 a HU-12, ADR-0001, ADR-0002).
 */
public interface HospitalReservationRepository {

    boolean existsByEmergencyId(EmergencyId emergencyId);

    void createReserved(
            String reservationId,
            EmergencyId emergencyId,
            HospitalId hospitalId,
            long reservedAtTicks
    );

    Optional<HospitalReservation> findById(String reservationId);

    Optional<HospitalReservation> findByIdForUpdate(String reservationId);

    Optional<HospitalReservation> findByEmergencyIdForUpdate(EmergencyId emergencyId);

    List<HospitalReservation> findAllOrderByReservedAtAndId();

    HospitalReservation save(HospitalReservation reservation);
}

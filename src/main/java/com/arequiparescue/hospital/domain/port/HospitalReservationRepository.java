package com.arequiparescue.hospital.domain.port;

import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.hospital.domain.HospitalId;

/**
 * Puerto de dominio para persistir y validar unicidad de reservas hospitalarias activas
 * sin acoplar el dominio a JPA (HU-09, HU-10, ADR-0001, ADR-0002).
 */
public interface HospitalReservationRepository {

    boolean existsByEmergencyId(EmergencyId emergencyId);

    void createReserved(
            String reservationId,
            EmergencyId emergencyId,
            HospitalId hospitalId,
            long reservedAtTicks
    );
}

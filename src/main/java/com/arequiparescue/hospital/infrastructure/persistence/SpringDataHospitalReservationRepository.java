package com.arequiparescue.hospital.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataHospitalReservationRepository extends JpaRepository<HospitalReservationJpaEntity, String> {

    boolean existsByEmergencyId(String emergencyId);

    Optional<HospitalReservationJpaEntity> findByEmergencyId(String emergencyId);
}

package com.arequiparescue.hospital.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataHospitalReservationRepository extends JpaRepository<HospitalReservationJpaEntity, String> {

    boolean existsByEmergencyId(String emergencyId);

    Optional<HospitalReservationJpaEntity> findByEmergencyId(String emergencyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM HospitalReservationJpaEntity r WHERE r.id = :id")
    Optional<HospitalReservationJpaEntity> findByIdForUpdate(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM HospitalReservationJpaEntity r WHERE r.emergencyId = :emergencyId")
    Optional<HospitalReservationJpaEntity> findByEmergencyIdForUpdate(@Param("emergencyId") String emergencyId);

    List<HospitalReservationJpaEntity> findAllByOrderByReservedAtTicksAscIdAsc();
}

package com.arequiparescue.hospital.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataHospitalRepository extends JpaRepository<HospitalJpaEntity, String> {

    List<HospitalJpaEntity> findAllByOrderByIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM HospitalJpaEntity h WHERE h.id = :id")
    Optional<HospitalJpaEntity> findByIdForUpdate(@Param("id") String id);
}

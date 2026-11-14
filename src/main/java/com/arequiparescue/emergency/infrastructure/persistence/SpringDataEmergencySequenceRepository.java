package com.arequiparescue.emergency.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SpringDataEmergencySequenceRepository extends JpaRepository<EmergencySequenceJpaEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM EmergencySequenceJpaEntity s WHERE s.sequenceName = :sequenceName")
    Optional<EmergencySequenceJpaEntity> findByNameForUpdate(@Param("sequenceName") String sequenceName);
}

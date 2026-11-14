package com.arequiparescue.emergency.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataEmergencyRepository extends JpaRepository<EmergencyJpaEntity, String> {

    List<EmergencyJpaEntity> findAllByOrderByRegisteredAtTicksAscIdAsc();

    List<EmergencyJpaEntity> findByStateOrderByQueueEnteredAtTicksAscIdAsc(String state);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EmergencyJpaEntity e WHERE e.id = :id")
    Optional<EmergencyJpaEntity> findByIdForUpdate(@Param("id") String id);
}

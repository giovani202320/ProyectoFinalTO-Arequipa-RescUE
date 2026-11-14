package com.arequiparescue.ambulance.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataAmbulanceRepository extends JpaRepository<AmbulanceJpaEntity, String> {

    List<AmbulanceJpaEntity> findAllByOrderByIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AmbulanceJpaEntity a WHERE a.id = :id")
    Optional<AmbulanceJpaEntity> findByIdForUpdate(@Param("id") String id);
}

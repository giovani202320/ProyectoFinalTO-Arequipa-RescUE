package com.arequiparescue.emergency.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataEmergencyRepository extends JpaRepository<EmergencyJpaEntity, String> {

    List<EmergencyJpaEntity> findAllByOrderByRegisteredAtTicksAscIdAsc();
}

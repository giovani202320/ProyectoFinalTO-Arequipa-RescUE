package com.arequiparescue.emergency.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataEmergencyRouteSegmentRepository
        extends JpaRepository<EmergencyRouteSegmentJpaEntity, Long> {

    boolean existsByEmergencyIdAndSegmentType(String emergencyId, String segmentType);

    List<EmergencyRouteSegmentJpaEntity> findByEmergencyIdOrderByIdAsc(String emergencyId);

    Optional<EmergencyRouteSegmentJpaEntity> findByEmergencyIdAndSegmentType(String emergencyId, String segmentType);
}

package com.arequiparescue.emergency.infrastructure.persistence;

import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.emergency.domain.EmergencyRouteSegment;
import com.arequiparescue.emergency.domain.RouteSegmentType;
import com.arequiparescue.emergency.domain.port.EmergencyRouteSegmentRepository;
import com.arequiparescue.routing.domain.NodeId;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador JPA de persistencia para segmentos históricos de ruta Dijkstra (ADR-0003).
 */
@Repository
public class EmergencyRouteSegmentJpaAdapter implements EmergencyRouteSegmentRepository {

    private final SpringDataEmergencyRouteSegmentRepository jpaRepository;

    public EmergencyRouteSegmentJpaAdapter(SpringDataEmergencyRouteSegmentRepository jpaRepository) {
        this.jpaRepository = Objects.requireNonNull(jpaRepository);
    }

    @Override
    public void saveIfAbsent(EmergencyRouteSegment segment) {
        Objects.requireNonNull(segment, "El segmento de ruta no puede ser nulo.");
        if (jpaRepository.existsByEmergencyIdAndSegmentType(
                segment.emergencyId().value(), segment.segmentType().name())) {
            return;
        }
        String serializedPath = segment.pathNodes().stream()
                .map(NodeId::value)
                .collect(Collectors.joining(","));
        jpaRepository.save(new EmergencyRouteSegmentJpaEntity(
                segment.emergencyId().value(),
                segment.segmentType().name(),
                segment.resourceId(),
                segment.originNodeId().value(),
                segment.destinationNodeId().value(),
                serializedPath,
                segment.abstractCost(),
                segment.calculatedAtTicks()
        ));
    }

    @Override
    public List<EmergencyRouteSegment> findByEmergencyId(EmergencyId emergencyId) {
        Objects.requireNonNull(emergencyId, "El identificador de emergencia es obligatorio.");
        return jpaRepository.findByEmergencyIdOrderByIdAsc(emergencyId.value()).stream()
                .map(EmergencyRouteSegmentJpaAdapter::toDomain)
                .toList();
    }

    @Override
    public Optional<EmergencyRouteSegment> findByEmergencyIdAndType(
            EmergencyId emergencyId,
            RouteSegmentType segmentType
    ) {
        Objects.requireNonNull(emergencyId, "El identificador de emergencia es obligatorio.");
        Objects.requireNonNull(segmentType, "El tipo de segmento es obligatorio.");
        return jpaRepository.findByEmergencyIdAndSegmentType(emergencyId.value(), segmentType.name())
                .map(EmergencyRouteSegmentJpaAdapter::toDomain);
    }

    private static EmergencyRouteSegment toDomain(EmergencyRouteSegmentJpaEntity entity) {
        List<NodeId> nodes = Arrays.stream(entity.getPathNodes().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(NodeId::of)
                .toList();
        return new EmergencyRouteSegment(
                EmergencyId.of(entity.getEmergencyId()),
                RouteSegmentType.valueOf(entity.getSegmentType()),
                entity.getResourceId(),
                NodeId.of(entity.getOriginNodeId()),
                NodeId.of(entity.getDestinationNodeId()),
                nodes,
                entity.getAbstractCost(),
                entity.getCalculatedAtT()
        );
    }
}

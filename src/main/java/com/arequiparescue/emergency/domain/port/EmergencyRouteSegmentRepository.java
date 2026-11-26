package com.arequiparescue.emergency.domain.port;

import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.emergency.domain.EmergencyRouteSegment;
import com.arequiparescue.emergency.domain.RouteSegmentType;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de dominio para persistencia inmutable y consulta de segmentos históricos de ruta (ADR-0003).
 */
public interface EmergencyRouteSegmentRepository {

    /**
     * Persiste el segmento únicamente si aún no existe un registro para {@code (emergencyId, segmentType)},
     * garantizando inmutabilidad histórica y ausencia de duplicados ante reintentos.
     */
    void saveIfAbsent(EmergencyRouteSegment segment);

    List<EmergencyRouteSegment> findByEmergencyId(EmergencyId emergencyId);

    Optional<EmergencyRouteSegment> findByEmergencyIdAndType(EmergencyId emergencyId, RouteSegmentType segmentType);
}

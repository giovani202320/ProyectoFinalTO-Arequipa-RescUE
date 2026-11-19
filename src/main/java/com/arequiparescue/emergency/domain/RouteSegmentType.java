package com.arequiparescue.emergency.domain;

/**
 * Tipo de segmento histórico de ruta Dijkstra persistido para una emergencia (ADR-0003).
 */
public enum RouteSegmentType {
    AMBULANCE_TO_INCIDENT,
    INCIDENT_TO_HOSPITAL
}

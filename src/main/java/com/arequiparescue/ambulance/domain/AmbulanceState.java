package com.arequiparescue.ambulance.domain;

/**
 * Conjunto cerrado de exactamente 4 estados operativos aprobados para una ambulancia
 * en el simulador ARE (DEC-019, RN-RUT-007, INV-SM-01).
 * No existen estados adicionales como EN_ROUTE, DISPATCHED, WAITING_HOSPITAL o MAINTENANCE.
 */
public enum AmbulanceState {

    AVAILABLE("Disponible", "Unidad disponible en su nodo actual para evaluación de elegibilidad y despacho."),
    ASSIGNED("Asignada", "Asignada de forma exclusiva a una emergencia activa y desplazándose hacia el incidente."),
    AT_INCIDENT("En nodo del incidente", "En el nodo del incidente durante recogida/reserva hospitalaria o retenida en espera hospitalaria."),
    TRANSPORTING("En traslado", "Trasladando a su emergencia vinculada hacia el hospital reservado.");

    private final String displayLabel;
    private final String operationalDescription;

    AmbulanceState(String displayLabel, String operationalDescription) {
        this.displayLabel = displayLabel;
        this.operationalDescription = operationalDescription;
    }

    public String displayLabel() {
        return displayLabel;
    }

    public String operationalDescription() {
        return operationalDescription;
    }
}

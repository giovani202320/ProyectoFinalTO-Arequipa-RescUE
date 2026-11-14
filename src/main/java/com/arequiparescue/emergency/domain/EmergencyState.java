package com.arequiparescue.emergency.domain;

/**
 * Conjunto cerrado de exactamente 7 estados aprobados para el ciclo de vida de una emergencia
 * en el simulador ARE (DEC-019, RN-RUT-007, INV-SM-01).
 * No existen estados adicionales como TRIAGED, DISPATCHED o EN_ROUTE_TO_INCIDENT.
 */
public enum EmergencyState {

    REGISTERED(
            "Registrada",
            "Emergencia registrada y clasificada determinísticamente según la versión activa de reglas."
    ),
    WAITING_AMBULANCE(
            "En espera de ambulancia",
            "Emergencia en cola FIFO de espera de ambulancia sin unidad activamente vinculada."
    ),
    ASSIGNED(
            "Ambulancia asignada",
            "Ambulancia confirmada y desplazándose hacia el nodo del incidente."
    ),
    AT_INCIDENT(
            "En nodo del incidente",
            "Ambulancia en el nodo del incidente con recogida confirmada, evaluando reserva hospitalaria."
    ),
    WAITING_HOSPITAL(
            "En espera de destino hospitalario",
            "Emergencia en cola FIFO esperando plaza hospitalaria; conserva su ambulancia asignada y retenida en estado AT_INCIDENT."
    ),
    IN_TRANSPORT(
            "En traslado a hospital",
            "Destino hospitalario y reserva de plaza confirmados; ambulancia trasladando la emergencia hacia el hospital."
    ),
    COMPLETED(
            "Completada",
            "Llegada al hospital reservado y entrega simulada completada. Ciclo prehospitalario cerrado."
    );

    private final String displayLabel;
    private final String operationalDescription;

    EmergencyState(String displayLabel, String operationalDescription) {
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

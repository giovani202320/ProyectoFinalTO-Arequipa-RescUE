package com.arequiparescue.simulation.domain;

/**
 * Categorías canónicas de eventos discretos y su precedencia fija de desempate
 * ante igualdad exacta de tiempo lógico (t_a = t_b), conforme a DEC-019, RN-RUT-009 y CA-SIM-07.
 */
public enum DiscreteEventType {

    /**
     * 1.° Liberación de recursos (entrega simulada en hospital / alta simulada hospitalaria).
     */
    RESOURCE_RELEASE(1),

    /**
     * 2.° Llegada de ambulancia al nodo del incidente y confirmación de recogida.
     */
    AMBULANCE_ARRIVAL_AT_INCIDENT(2),

    /**
     * 3.° Evaluación y asignación/reserva de colas en espera (WAITING_AMBULANCE / WAITING_HOSPITAL).
     */
    WAITING_QUEUE_EVALUATION(3),

    /**
     * 4.° Registro de nueva solicitud de emergencia.
     */
    EMERGENCY_REGISTRATION(4);

    private final int precedenceRank;

    DiscreteEventType(int precedenceRank) {
        this.precedenceRank = precedenceRank;
    }

    public int precedenceRank() {
        return precedenceRank;
    }
}

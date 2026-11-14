package com.arequiparescue.ambulance.domain;

import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.emergency.domain.EquipmentType;
import com.arequiparescue.routing.domain.NodeId;

import java.util.Objects;

/**
 * Agregado de dominio puro que representa una ambulancia del catálogo operativo de ARE
 * (HU-03, DEC-016, DEC-019, RN-EME-007, RN-RUT-007, INV-SM-01, INV-SM-02).
 * Libre de dependencias de Spring, JPA, Hibernate o Servlet.
 */
public final class Ambulance {

    private final AmbulanceId id;
    private final EquipmentType equipmentType;
    private final NodeId currentNode;
    private final AmbulanceState state;
    private final EmergencyId assignedEmergencyId;

    private Ambulance(
            AmbulanceId id,
            EquipmentType equipmentType,
            NodeId currentNode,
            AmbulanceState state,
            EmergencyId assignedEmergencyId
    ) {
        this.id = Objects.requireNonNull(id, "El identificador de la ambulancia es obligatorio.");
        this.equipmentType = Objects.requireNonNull(
                equipmentType, "El equipamiento de la ambulancia es obligatorio (BASIC o SPECIALIZED; RN-EME-007).");
        this.currentNode = Objects.requireNonNull(
                currentNode, "El nodo de ubicación actual de la ambulancia es obligatorio (N01 a N10; RN-RUT-006).");
        this.state = Objects.requireNonNull(
                state, "El estado de la ambulancia es obligatorio y debe pertenecer a los 4 estados de DEC-019.");
        this.assignedEmergencyId = assignedEmergencyId;

        validateStateBindingInvariants(this.state, this.assignedEmergencyId);
    }

    /**
     * Registra una nueva ambulancia en el catálogo administrativo en estado inicial {@link AmbulanceState#AVAILABLE}
     * sin emergencia vinculada (transición T-AMB-00, HU-03 CA-1).
     */
    public static Ambulance registerInCatalog(
            AmbulanceId id,
            EquipmentType equipmentType,
            NodeId initialNode
    ) {
        return new Ambulance(id, equipmentType, initialNode, AmbulanceState.AVAILABLE, null);
    }

    /**
     * Reconstruye la ambulancia desde la persistencia validando todas las invariantes (INV-SM-01, INV-SM-02).
     */
    public static Ambulance reconstruct(
            AmbulanceId id,
            EquipmentType equipmentType,
            NodeId currentNode,
            AmbulanceState state,
            EmergencyId assignedEmergencyId
    ) {
        return new Ambulance(id, equipmentType, currentNode, state, assignedEmergencyId);
    }

    /**
     * Actualiza la configuración administrativa (equipamiento y nodo actual) de una ambulancia fuera de
     * escenarios activos (HU-03 CA-1).
     */
    public Ambulance updateCatalogConfiguration(
            EquipmentType newEquipmentType,
            NodeId newCurrentNode,
            boolean activeScenarioInProgress
    ) {
        if (activeScenarioInProgress) {
            throw new IllegalStateException(
                    "No se permite modificar el catálogo de ambulancias mientras un escenario experimental está activo (HU-03 CA-1).");
        }
        if (this.state != AmbulanceState.AVAILABLE) {
            throw new IllegalStateException(
                    "No se puede reconfigurar administrativamente la ambulancia " + this.id
                            + " porque se encuentra ocupada en estado " + this.state + ".");
        }
        return new Ambulance(this.id, newEquipmentType, newCurrentNode, this.state, this.assignedEmergencyId);
    }

    private static void validateStateBindingInvariants(AmbulanceState state, EmergencyId assignedEmergencyId) {
        if (state == AmbulanceState.AVAILABLE && assignedEmergencyId != null) {
            throw new IllegalArgumentException(
                    "Violación de INV-SM-02: una ambulancia en estado AVAILABLE no puede tener una emergencia activamente vinculada.");
        }
        if (state != AmbulanceState.AVAILABLE && assignedEmergencyId == null) {
            throw new IllegalArgumentException(
                    "Violación de INV-SM-02: una ambulancia en estado " + state
                            + " requiere mantener asignado el identificador de su emergencia activa.");
        }
    }

    /**
     * Ejecuta la transición T-AMB-01 (AVAILABLE -> ASSIGNED) vinculando la emergencia activa
     * bajo invariante INV-SM-02 (HU-06 CA-1).
     */
    public Ambulance assignToEmergency(EmergencyId emergencyId) {
        Objects.requireNonNull(emergencyId, "El identificador de la emergencia a asignar es obligatorio.");
        if (this.state != AmbulanceState.AVAILABLE || this.assignedEmergencyId != null) {
            throw new IllegalStateException(
                    "La ambulancia " + this.id + " no está disponible para asignación. Estado actual: " + this.state);
        }
        return new Ambulance(
                this.id,
                this.equipmentType,
                this.currentNode,
                AmbulanceState.ASSIGNED,
                emergencyId
        );
    }

    /**
     * Ejecuta la transición T-AMB-02 (ASSIGNED -> AT_INCIDENT) actualizando la ubicación al nodo del incidente
     * y conservando el vínculo con la emergencia activa (HU-09, INV-SM-02, INV-SM-03).
     */
    public Ambulance arriveAtIncident(NodeId incidentNode) {
        Objects.requireNonNull(incidentNode, "El nodo del incidente es obligatorio.");
        if (this.state != AmbulanceState.ASSIGNED || this.assignedEmergencyId == null) {
            throw new IllegalStateException(
                    "Solo una ambulancia en estado ASSIGNED puede llegar al incidente. Estado actual: " + this.state);
        }
        return new Ambulance(
                this.id,
                this.equipmentType,
                incidentNode,
                AmbulanceState.AT_INCIDENT,
                this.assignedEmergencyId
        );
    }

    /**
     * Ejecuta la transición T-AMB-03 (AT_INCIDENT -> TRANSPORTING) al confirmarse una reserva hospitalaria
     * (HU-09, HU-10, INV-SM-03).
     */
    public Ambulance startTransportToHospital() {
        if (this.state != AmbulanceState.AT_INCIDENT || this.assignedEmergencyId == null) {
            throw new IllegalStateException(
                    "Solo una ambulancia en estado AT_INCIDENT puede iniciar traslado a hospital. Estado actual: " + this.state);
        }
        return new Ambulance(
                this.id,
                this.equipmentType,
                this.currentNode,
                AmbulanceState.TRANSPORTING,
                this.assignedEmergencyId
        );
    }

    public AmbulanceId id() {
        return id;
    }

    public EquipmentType equipmentType() {
        return equipmentType;
    }

    public NodeId currentNode() {
        return currentNode;
    }

    public AmbulanceState state() {
        return state;
    }

    public EmergencyId assignedEmergencyId() {
        return assignedEmergencyId;
    }
}

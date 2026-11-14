package com.arequiparescue.ambulance.presentation;

import jakarta.validation.constraints.NotBlank;

/**
 * Formulario para registrar o actualizar una ambulancia en el catálogo (HU-03).
 */
public class AmbulanceCatalogForm {

    private String id = "";

    @NotBlank(message = "El equipamiento de la ambulancia es obligatorio (BASIC o SPECIALIZED).")
    private String equipmentType = "";

    @NotBlank(message = "El nodo de ubicación en el grafo es obligatorio (N01 a N10).")
    private String nodeId = "";

    public AmbulanceCatalogForm() {
    }

    public AmbulanceCatalogForm(String id, String equipmentType, String nodeId) {
        this.id = id;
        this.equipmentType = equipmentType;
        this.nodeId = nodeId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEquipmentType() {
        return equipmentType;
    }

    public void setEquipmentType(String equipmentType) {
        this.equipmentType = equipmentType;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
}

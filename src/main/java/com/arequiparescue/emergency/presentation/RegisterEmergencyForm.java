package com.arequiparescue.emergency.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Formulario de entrada para el registro de una emergencia simulada (HU-01, RN-EME-009).
 * Utiliza exclusivamente los tres campos aprobados: categoría del incidente, nodo del grafo y descripción ficticia opcional.
 */
public class RegisterEmergencyForm {

    @NotBlank(message = "La categoría del incidente es obligatoria (seleccione INC-A, INC-B, INC-C o INC-D).")
    private String incidentCategory = "";

    @NotBlank(message = "El nodo de ubicación en el grafo es obligatorio (seleccione un nodo entre N01 y N10).")
    private String nodeId = "";

    @Size(max = 500, message = "La descripción ficticia no puede exceder los 500 caracteres.")
    private String description = "";

    public RegisterEmergencyForm() {
    }

    public RegisterEmergencyForm(String incidentCategory, String nodeId, String description) {
        this.incidentCategory = incidentCategory;
        this.nodeId = nodeId;
        this.description = description;
    }

    public String getIncidentCategory() {
        return incidentCategory;
    }

    public void setIncidentCategory(String incidentCategory) {
        this.incidentCategory = incidentCategory;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

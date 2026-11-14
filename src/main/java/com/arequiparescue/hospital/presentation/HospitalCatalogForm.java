package com.arequiparescue.hospital.presentation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Formulario web para registrar o actualizar un hospital en el catálogo (HU-03, DEC-018, RN-HOS-004).
 */
public class HospitalCatalogForm {

    private String id;

    @NotBlank(message = "El nombre descriptivo del hospital es obligatorio.")
    @Size(max = 120, message = "El nombre del hospital no puede exceder los 120 caracteres.")
    private String name;

    @NotBlank(message = "El nodo de ubicación del hospital es obligatorio (N01 a N10).")
    private String nodeId;

    private boolean enabled = true;

    @NotNull(message = "La capacidad numérica total simulada C del hospital es obligatoria.")
    @Min(value = 0, message = "La capacidad total simulada C del hospital debe ser mayor o igual a 0 (C >= 0).")
    private Integer totalCapacity = 0;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(Integer totalCapacity) {
        this.totalCapacity = totalCapacity;
    }
}

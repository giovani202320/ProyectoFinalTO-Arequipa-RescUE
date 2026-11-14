package com.arequiparescue.hospital.domain;

import com.arequiparescue.routing.domain.NodeId;

import java.util.Objects;

/**
 * Agregado de dominio puro que representa un hospital del simulador ARE con capacidad numérica
 * simulada C, R, O, D y sin especialidades clínicas (DEC-018, RN-HOS-001, RN-HOS-004, RN-HOS-015,
 * CA-HOS-12, INV-SM-04, HU-03).
 * Libre de dependencias de Spring, JPA, Hibernate o Servlet.
 */
public final class Hospital {

    private final HospitalId id;
    private final String name;
    private final NodeId locationNode;
    private final boolean enabled;
    private final int totalCapacity;
    private final int reservedSlots;
    private final int occupiedSlots;

    private Hospital(
            HospitalId id,
            String name,
            NodeId locationNode,
            boolean enabled,
            int totalCapacity,
            int reservedSlots,
            int occupiedSlots
    ) {
        this.id = Objects.requireNonNull(id, "El identificador del hospital es obligatorio.");
        this.name = validateName(name);
        this.locationNode = Objects.requireNonNull(
                locationNode, "El nodo de ubicación del hospital en el grafo es obligatorio (N01 a N10; RN-RUT-006).");
        this.enabled = enabled;
        validateCapacityInvariants(totalCapacity, reservedSlots, occupiedSlots);
        this.totalCapacity = totalCapacity;
        this.reservedSlots = reservedSlots;
        this.occupiedSlots = occupiedSlots;
    }

    /**
     * Registra un nuevo hospital en el catálogo administrativo preservando C >= 0, R >= 0, O >= 0 y R + O <= C
     * sin exigir ni asignar especialidades clínicas (HU-03 CA-1, RN-HOS-004).
     */
    public static Hospital registerInCatalog(
            HospitalId id,
            String name,
            NodeId locationNode,
            boolean enabled,
            int totalCapacity
    ) {
        return new Hospital(id, name, locationNode, enabled, totalCapacity, 0, 0);
    }

    public static Hospital reconstruct(
            HospitalId id,
            String name,
            NodeId locationNode,
            boolean enabled,
            int totalCapacity,
            int reservedSlots,
            int occupiedSlots
    ) {
        return new Hospital(id, name, locationNode, enabled, totalCapacity, reservedSlots, occupiedSlots);
    }

    /**
     * Actualiza la configuración administrativa del hospital aplicando el bloqueo de cambios de
     * capacidad total C durante escenarios experimentales activos (RN-HOS-015, CA-HOS-12, HU-03 CA-2)
     * y preservando R + O <= C fuera de escenarios activos (CA-HOS-12, HU-03 CA-3).
     */
    public Hospital updateCatalogConfiguration(
            String newName,
            NodeId newLocationNode,
            boolean newEnabled,
            int newTotalCapacity,
            boolean activeScenarioInProgress
    ) {
        if (activeScenarioInProgress) {
            if (newTotalCapacity != this.totalCapacity) {
                throw new IllegalStateException(
                        "Operación rechazada: se prohíbe modificar la capacidad total C del hospital " + this.id
                                + " durante la ejecución de un escenario experimental activo (RN-HOS-015, CA-HOS-12, HU-03 CA-2).");
            }
            throw new IllegalStateException(
                    "Operación rechazada: no se permite modificar el catálogo hospitalario mientras un escenario experimental está activo (HU-03 CA-1, RN-HOS-015).");
        }
        validateCapacityInvariants(newTotalCapacity, this.reservedSlots, this.occupiedSlots);
        return new Hospital(
                this.id,
                newName,
                newLocationNode,
                newEnabled,
                newTotalCapacity,
                this.reservedSlots,
                this.occupiedSlots
        );
    }

    private static String validateName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw new IllegalArgumentException("El nombre descriptivo del hospital es obligatorio.");
        }
        String trimmed = rawName.trim();
        if (trimmed.length() > 120) {
            throw new IllegalArgumentException("El nombre del hospital no puede exceder los 120 caracteres.");
        }
        return trimmed;
    }

    private static void validateCapacityInvariants(int totalCapacity, int reservedSlots, int occupiedSlots) {
        if (totalCapacity < 0) {
            throw new IllegalArgumentException(
                    "La capacidad total C del hospital debe ser un entero no negativo (C >= 0; RN-HOS-004). Valor recibido: "
                            + totalCapacity);
        }
        if (reservedSlots < 0) {
            throw new IllegalArgumentException(
                    "Las plazas reservadas R del hospital no pueden ser negativas (R >= 0; RN-HOS-004). Valor recibido: "
                            + reservedSlots);
        }
        if (occupiedSlots < 0) {
            throw new IllegalArgumentException(
                    "Las plazas ocupadas O del hospital no pueden ser negativas (O >= 0; RN-HOS-004). Valor recibido: "
                            + occupiedSlots);
        }
        int committed = Math.addExact(reservedSlots, occupiedSlots);
        if (committed > totalCapacity) {
            throw new IllegalArgumentException(
                    "Violación de invariante de capacidad hospitalaria (RN-HOS-004, CA-HOS-12): no se permite fijar C = "
                            + totalCapacity + " menor que las plazas comprometidas R + O = " + committed
                            + " (R=" + reservedSlots + ", O=" + occupiedSlots + ").");
        }
    }

    public HospitalId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public NodeId locationNode() {
        return locationNode;
    }

    public boolean enabled() {
        return enabled;
    }

    /**
     * Capacidad total simulada {@code C >= 0}.
     */
    public int totalCapacity() {
        return totalCapacity;
    }

    /**
     * Plazas reservadas {@code R >= 0}.
     */
    public int reservedSlots() {
        return reservedSlots;
    }

    /**
     * Plazas ocupadas {@code O >= 0}.
     */
    public int occupiedSlots() {
        return occupiedSlots;
    }

    /**
     * Plazas disponibles {@code D = C - R - O >= 0} (RN-HOS-004).
     */
    public int availableSlots() {
        return totalCapacity - reservedSlots - occupiedSlots;
    }
}

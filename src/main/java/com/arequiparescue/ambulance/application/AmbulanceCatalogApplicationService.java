package com.arequiparescue.ambulance.application;

import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.ambulance.domain.AmbulanceId;
import com.arequiparescue.ambulance.domain.port.AmbulanceRepository;
import com.arequiparescue.emergency.domain.EquipmentType;
import com.arequiparescue.routing.domain.NodeId;
import com.arequiparescue.routing.domain.RoadGraph;
import com.arequiparescue.routing.domain.port.RoadGraphRepository;
import com.arequiparescue.simulation.domain.port.ActiveScenarioChecker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Servicio de aplicación para administrar y consultar el catálogo de ambulancias sobre el grafo
 * de 10 nodos (HU-03, DEC-009 ACT-03, DEC-016, DEC-019, ADR-0001, ADR-0002).
 */
@Service
public class AmbulanceCatalogApplicationService {

    private final AmbulanceRepository ambulanceRepository;
    private final RoadGraphRepository roadGraphRepository;
    private final ActiveScenarioChecker activeScenarioChecker;

    public AmbulanceCatalogApplicationService(
            AmbulanceRepository ambulanceRepository,
            RoadGraphRepository roadGraphRepository,
            ActiveScenarioChecker activeScenarioChecker
    ) {
        this.ambulanceRepository = Objects.requireNonNull(ambulanceRepository);
        this.roadGraphRepository = Objects.requireNonNull(roadGraphRepository);
        this.activeScenarioChecker = Objects.requireNonNull(activeScenarioChecker);
    }

    @Transactional
    public Ambulance registerAmbulance(RegisterAmbulanceCommand command) {
        if (command == null) {
            throw new AmbulanceCatalogValidationException("global", "Los datos de la ambulancia son obligatorios.");
        }
        if (activeScenarioChecker.isAnyScenarioActiveForUpdate()) {
            throw new AmbulanceCatalogValidationException(
                    "global",
                    "Operación rechazada: no se permite registrar ni modificar ambulancias mientras un escenario experimental está activo (HU-03 CA-1)."
            );
        }

        EquipmentType equipmentType = parseEquipment(command.equipmentTypeCode());
        NodeId nodeId = parseAndValidateNode(command.nodeIdCode());
        AmbulanceId ambulanceId = resolveOrGenerateId(command.id());

        if (ambulanceRepository.existsById(ambulanceId)) {
            throw new AmbulanceCatalogValidationException(
                    "id",
                    "Ya existe una ambulancia registrada con el identificador " + ambulanceId.value() + "."
            );
        }

        Ambulance ambulance = Ambulance.registerInCatalog(ambulanceId, equipmentType, nodeId);
        return ambulanceRepository.save(ambulance);
    }

    @Transactional
    public Ambulance updateAmbulance(UpdateAmbulanceCommand command) {
        if (command == null || command.id() == null || command.id().isBlank()) {
            throw new AmbulanceCatalogValidationException("id", "El identificador de la ambulancia a actualizar es obligatorio.");
        }
        boolean scenarioActive = activeScenarioChecker.isAnyScenarioActiveForUpdate();

        final AmbulanceId ambulanceId;
        try {
            ambulanceId = AmbulanceId.of(command.id().trim());
        } catch (IllegalArgumentException ex) {
            throw new AmbulanceCatalogValidationException("id", ex.getMessage());
        }

        Ambulance existing = ambulanceRepository.findByIdForUpdate(ambulanceId)
                .orElseThrow(() -> new AmbulanceCatalogValidationException(
                        "id",
                        "No existe ninguna ambulancia con el identificador " + ambulanceId.value() + "."
                ));

        EquipmentType equipmentType = parseEquipment(command.equipmentTypeCode());
        NodeId nodeId = parseAndValidateNode(command.nodeIdCode());

        final Ambulance updated;
        try {
            updated = existing.updateCatalogConfiguration(equipmentType, nodeId, scenarioActive);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new AmbulanceCatalogValidationException("global", ex.getMessage());
        }

        return ambulanceRepository.save(updated);
    }

    @Transactional(readOnly = true)
    public List<Ambulance> listAmbulances() {
        return ambulanceRepository.findAllOrderById();
    }

    private AmbulanceId resolveOrGenerateId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return AmbulanceId.fromSequence(ambulanceRepository.nextSequenceNumber());
        }
        try {
            return AmbulanceId.of(rawId.trim());
        } catch (IllegalArgumentException ex) {
            throw new AmbulanceCatalogValidationException("id", ex.getMessage());
        }
    }

    private static EquipmentType parseEquipment(String rawEquipment) {
        if (rawEquipment == null || rawEquipment.isBlank()) {
            throw new AmbulanceCatalogValidationException(
                    "equipmentType",
                    "El equipamiento de la ambulancia es obligatorio (BASIC o SPECIALIZED; HU-03 CA-1)."
            );
        }
        try {
            return EquipmentType.valueOf(rawEquipment.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new AmbulanceCatalogValidationException(
                    "equipmentType",
                    "El equipamiento '" + rawEquipment.trim() + "' no es válido. Use BASIC o SPECIALIZED (RN-EME-007)."
            );
        }
    }

    private NodeId parseAndValidateNode(String rawNodeId) {
        if (rawNodeId == null || rawNodeId.isBlank()) {
            throw new AmbulanceCatalogValidationException(
                    "nodeId",
                    "El nodo de ubicación de la ambulancia es obligatorio (N01 a N10; HU-03 CA-1)."
            );
        }
        final NodeId nodeId;
        try {
            nodeId = NodeId.of(rawNodeId.trim());
        } catch (IllegalArgumentException ex) {
            throw new AmbulanceCatalogValidationException("nodeId", ex.getMessage());
        }
        RoadGraph graph = roadGraphRepository.loadGraph();
        boolean exists = graph.nodes().stream().anyMatch(n -> n.id().equals(nodeId));
        if (!exists) {
            throw new AmbulanceCatalogValidationException(
                    "nodeId",
                    "El nodo '" + nodeId.value() + "' no existe en el grafo configurado (N01 a N10)."
            );
        }
        return nodeId;
    }
}

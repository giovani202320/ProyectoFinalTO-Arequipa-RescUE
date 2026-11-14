package com.arequiparescue.hospital.application;

import com.arequiparescue.hospital.domain.Hospital;
import com.arequiparescue.hospital.domain.HospitalId;
import com.arequiparescue.hospital.domain.port.HospitalRepository;
import com.arequiparescue.routing.domain.NodeId;
import com.arequiparescue.routing.domain.RoadGraph;
import com.arequiparescue.routing.domain.port.RoadGraphRepository;
import com.arequiparescue.simulation.domain.port.ActiveScenarioChecker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Servicio de aplicación para administrar y consultar el catálogo de hospitales sobre el grafo
 * de 10 nodos (HU-03, DEC-009 ACT-03, DEC-018, RN-HOS-001, RN-HOS-004, RN-HOS-015, CA-HOS-12).
 */
@Service
public class HospitalCatalogApplicationService {

    private final HospitalRepository hospitalRepository;
    private final RoadGraphRepository roadGraphRepository;
    private final ActiveScenarioChecker activeScenarioChecker;

    public HospitalCatalogApplicationService(
            HospitalRepository hospitalRepository,
            RoadGraphRepository roadGraphRepository,
            ActiveScenarioChecker activeScenarioChecker
    ) {
        this.hospitalRepository = Objects.requireNonNull(hospitalRepository);
        this.roadGraphRepository = Objects.requireNonNull(roadGraphRepository);
        this.activeScenarioChecker = Objects.requireNonNull(activeScenarioChecker);
    }

    @Transactional
    public Hospital registerHospital(RegisterHospitalCommand command) {
        if (command == null) {
            throw new HospitalCatalogValidationException("global", "Los datos del hospital son obligatorios.");
        }
        if (activeScenarioChecker.isAnyScenarioActiveForUpdate()) {
            throw new HospitalCatalogValidationException(
                    "global",
                    "Operación rechazada: no se permite registrar hospitales mientras un escenario experimental está activo (HU-03 CA-1, RN-HOS-015)."
            );
        }
        if (command.totalCapacity() == null) {
            throw new HospitalCatalogValidationException(
                    "totalCapacity",
                    "La capacidad total simulada C del hospital es obligatoria (C >= 0; RN-HOS-004)."
            );
        }

        NodeId nodeId = parseAndValidateNode(command.nodeIdCode());
        HospitalId hospitalId = resolveOrGenerateId(command.id());

        if (hospitalRepository.existsById(hospitalId)) {
            throw new HospitalCatalogValidationException(
                    "id",
                    "Ya existe un hospital registrado con el identificador " + hospitalId.value() + "."
            );
        }

        final Hospital hospital;
        try {
            hospital = Hospital.registerInCatalog(
                    hospitalId,
                    command.name(),
                    nodeId,
                    command.enabled(),
                    command.totalCapacity()
            );
        } catch (IllegalArgumentException ex) {
            throw new HospitalCatalogValidationException("global", ex.getMessage());
        }

        return hospitalRepository.save(hospital);
    }

    @Transactional
    public Hospital updateHospital(UpdateHospitalCommand command) {
        if (command == null || command.id() == null || command.id().isBlank()) {
            throw new HospitalCatalogValidationException("id", "El identificador del hospital a actualizar es obligatorio.");
        }
        if (command.totalCapacity() == null) {
            throw new HospitalCatalogValidationException(
                    "totalCapacity",
                    "La capacidad total simulada C del hospital es obligatoria (C >= 0; RN-HOS-004)."
            );
        }

        boolean scenarioActive = activeScenarioChecker.isAnyScenarioActiveForUpdate();

        final HospitalId hospitalId;
        try {
            hospitalId = HospitalId.of(command.id().trim());
        } catch (IllegalArgumentException ex) {
            throw new HospitalCatalogValidationException("id", ex.getMessage());
        }

        Hospital existing = hospitalRepository.findByIdForUpdate(hospitalId)
                .orElseThrow(() -> new HospitalCatalogValidationException(
                        "id",
                        "No existe ningún hospital con el identificador " + hospitalId.value() + "."
                ));

        NodeId nodeId = parseAndValidateNode(command.nodeIdCode());

        final Hospital updated;
        try {
            updated = existing.updateCatalogConfiguration(
                    command.name(),
                    nodeId,
                    command.enabled(),
                    command.totalCapacity(),
                    scenarioActive
            );
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new HospitalCatalogValidationException("totalCapacity", ex.getMessage());
        }

        return hospitalRepository.save(updated);
    }

    @Transactional(readOnly = true)
    public List<Hospital> listHospitals() {
        return hospitalRepository.findAllOrderById();
    }

    private HospitalId resolveOrGenerateId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return HospitalId.fromSequence(hospitalRepository.nextSequenceNumber());
        }
        try {
            return HospitalId.of(rawId.trim());
        } catch (IllegalArgumentException ex) {
            throw new HospitalCatalogValidationException("id", ex.getMessage());
        }
    }

    private NodeId parseAndValidateNode(String rawNodeId) {
        if (rawNodeId == null || rawNodeId.isBlank()) {
            throw new HospitalCatalogValidationException(
                    "nodeId",
                    "El nodo de ubicación del hospital es obligatorio (N01 a N10; HU-03 CA-1)."
            );
        }
        final NodeId nodeId;
        try {
            nodeId = NodeId.of(rawNodeId.trim());
        } catch (IllegalArgumentException ex) {
            throw new HospitalCatalogValidationException("nodeId", ex.getMessage());
        }
        RoadGraph graph = roadGraphRepository.loadGraph();
        boolean exists = graph.nodes().stream().anyMatch(n -> n.id().equals(nodeId));
        if (!exists) {
            throw new HospitalCatalogValidationException(
                    "nodeId",
                    "El nodo '" + nodeId.value() + "' no existe en el grafo configurado (N01 a N10)."
            );
        }
        return nodeId;
    }
}

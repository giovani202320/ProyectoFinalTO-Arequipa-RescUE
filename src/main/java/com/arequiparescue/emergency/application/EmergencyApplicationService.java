package com.arequiparescue.emergency.application;

import com.arequiparescue.emergency.domain.DeterministicTriageService;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.emergency.domain.EmergencyRouteSegment;
import com.arequiparescue.emergency.domain.IncidentCategory;
import com.arequiparescue.emergency.domain.TriageClassification;
import com.arequiparescue.emergency.domain.TriageRuleVersion;
import com.arequiparescue.emergency.domain.port.EmergencyRepository;
import com.arequiparescue.emergency.domain.port.EmergencyRouteSegmentRepository;
import com.arequiparescue.emergency.domain.port.TriageRuleVersionRepository;
import com.arequiparescue.routing.domain.GraphNode;
import com.arequiparescue.routing.domain.NodeId;
import com.arequiparescue.routing.domain.RoadGraph;
import com.arequiparescue.routing.domain.port.RoadGraphRepository;
import com.arequiparescue.shared.domain.LogicalClock;
import com.arequiparescue.shared.domain.LogicalTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Servicio de aplicación que coordina los casos de uso de registro y clasificación determinista
 * de emergencias ({@code HU-01}) y consulta/seguimiento de estados ({@code HU-02}),
 * respetando las fronteras transaccionales de ADR-0001 Secc. 4.3.4.
 */
@Service
public class EmergencyApplicationService {

    private final EmergencyRepository emergencyRepository;
    private final TriageRuleVersionRepository triageRuleVersionRepository;
    private final RoadGraphRepository roadGraphRepository;
    private final LogicalClock logicalClock;
    private final EmergencyRouteSegmentRepository routeSegmentRepository;
    private final DeterministicTriageService deterministicTriageService;

    public EmergencyApplicationService(
            EmergencyRepository emergencyRepository,
            TriageRuleVersionRepository triageRuleVersionRepository,
            RoadGraphRepository roadGraphRepository,
            LogicalClock logicalClock
    ) {
        this(emergencyRepository, triageRuleVersionRepository, roadGraphRepository, logicalClock, null);
    }

    @Autowired
    public EmergencyApplicationService(
            EmergencyRepository emergencyRepository,
            TriageRuleVersionRepository triageRuleVersionRepository,
            RoadGraphRepository roadGraphRepository,
            LogicalClock logicalClock,
            EmergencyRouteSegmentRepository routeSegmentRepository
    ) {
        this.emergencyRepository = Objects.requireNonNull(emergencyRepository);
        this.triageRuleVersionRepository = Objects.requireNonNull(triageRuleVersionRepository);
        this.roadGraphRepository = Objects.requireNonNull(roadGraphRepository);
        this.logicalClock = Objects.requireNonNull(logicalClock);
        this.routeSegmentRepository = routeSegmentRepository;
        this.deterministicTriageService = new DeterministicTriageService();
    }

    @Transactional(readOnly = true)
    public List<EmergencyRouteSegment> findRouteSegments(EmergencyId emergencyId) {
        if (routeSegmentRepository == null || emergencyId == null) {
            return List.of();
        }
        return routeSegmentRepository.findByEmergencyId(emergencyId);
    }

    /**
     * Caso de uso HU-01: valida los campos de entrada, evalúa el triage determinista contra la versión
     * activa de reglas y persiste la emergencia en estado inicial {@code REGISTERED} en el instante lógico actual.
     */
    @Transactional
    public Emergency registerEmergency(RegisterEmergencyCommand command) {
        if (command == null) {
            throw new EmergencyValidationException("global", "Los datos de registro de la emergencia son obligatorios.");
        }

        IncidentCategory category = parseAndValidateCategory(command.incidentCategoryCode());
        NodeId locationNode = parseAndValidateLocationNode(command.locationNodeCode());
        String normalizedDescription = validateAndNormalizeDescription(command.description());

        TriageRuleVersion activeRuleVersion = triageRuleVersionRepository
                .findActiveVersion()
                .orElseThrow(() -> new EmergencyValidationException(
                        "incidentCategory",
                        "No existe una versión activa de reglas de clasificación en el sistema; no se aplican clasificaciones por defecto (RN-EME-010.5)."
                ));

        final TriageClassification classification;
        try {
            classification = deterministicTriageService.classify(category, activeRuleVersion);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new EmergencyValidationException("incidentCategory", ex.getMessage());
        }

        EmergencyId nextId = EmergencyId.fromSequence(emergencyRepository.nextSequenceNumber());
        LogicalTime registeredAt = logicalClock.currentTime();

        Emergency emergency = Emergency.register(
                nextId,
                locationNode,
                normalizedDescription,
                classification,
                registeredAt
        );

        return emergencyRepository.save(emergency);
    }

    /**
     * Caso de uso HU-02: lista todas las emergencias ordenadas por tiempo lógico de registro e identificador estable.
     */
    @Transactional(readOnly = true)
    public List<Emergency> listEmergencies() {
        return emergencyRepository.findAllOrderByRegisteredAtAndId();
    }

    /**
     * Caso de uso HU-02: obtiene el detalle completo de una emergencia por su identificador canónico.
     */
    @Transactional(readOnly = true)
    public Emergency getEmergencyById(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            throw new EmergencyNotFoundException(String.valueOf(rawId));
        }
        final EmergencyId id;
        try {
            id = EmergencyId.of(rawId.trim());
        } catch (IllegalArgumentException ex) {
            throw new EmergencyNotFoundException(rawId);
        }
        return emergencyRepository
                .findById(id)
                .orElseThrow(() -> new EmergencyNotFoundException(id.value()));
    }

    @Transactional(readOnly = true)
    public List<GraphNode> listConfiguredGraphNodes() {
        return roadGraphRepository.loadGraph().nodes();
    }

    @Transactional(readOnly = true)
    public Optional<TriageRuleVersion> findActiveRuleVersion() {
        return triageRuleVersionRepository.findActiveVersion();
    }

    public LogicalTime currentLogicalTime() {
        return logicalClock.currentTime();
    }

    private IncidentCategory parseAndValidateCategory(String rawCategoryCode) {
        if (rawCategoryCode == null || rawCategoryCode.isBlank()) {
            throw new EmergencyValidationException(
                    "incidentCategory",
                    "La categoría del incidente es obligatoria. Seleccione INC-A, INC-B, INC-C o INC-D (RN-EME-010.1)."
            );
        }
        try {
            return IncidentCategory.fromCode(rawCategoryCode);
        } catch (IllegalArgumentException ex) {
            throw new EmergencyValidationException("incidentCategory", ex.getMessage());
        }
    }

    private NodeId parseAndValidateLocationNode(String rawNodeCode) {
        if (rawNodeCode == null || rawNodeCode.isBlank()) {
            throw new EmergencyValidationException(
                    "nodeId",
                    "El nodo de ubicación en el grafo es obligatorio. Seleccione un nodo entre N01 y N10 (RN-EME-010.3)."
            );
        }
        final NodeId nodeId;
        try {
            nodeId = NodeId.of(rawNodeCode.trim());
        } catch (IllegalArgumentException ex) {
            throw new EmergencyValidationException(
                    "nodeId",
                    "El nodo de ubicación '" + rawNodeCode.trim()
                            + "' no es válido. Debe pertenecer al catálogo N01 a N10 del grafo configurado (RN-EME-010.3)."
            );
        }

        RoadGraph graph = roadGraphRepository.loadGraph();
        boolean existsInGraph = graph.nodes().stream().anyMatch(node -> node.id().equals(nodeId));
        if (!existsInGraph) {
            throw new EmergencyValidationException(
                    "nodeId",
                    "El nodo de ubicación '" + nodeId.value()
                            + "' no existe en el grafo vial configurado (RN-EME-010.3)."
            );
        }
        return nodeId;
    }

    private String validateAndNormalizeDescription(String rawDescription) {
        if (rawDescription == null || rawDescription.isBlank()) {
            return "";
        }
        String trimmed = rawDescription.trim();
        if (trimmed.length() > Emergency.MAX_DESCRIPTION_LENGTH) {
            throw new EmergencyValidationException(
                    "description",
                    "La descripción ficticia no puede exceder los " + Emergency.MAX_DESCRIPTION_LENGTH + " caracteres."
            );
        }
        return trimmed;
    }
}

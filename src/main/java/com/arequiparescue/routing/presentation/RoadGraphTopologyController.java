package com.arequiparescue.routing.presentation;

import com.arequiparescue.ambulance.application.AmbulanceCatalogApplicationService;
import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.ambulance.domain.AmbulanceState;
import com.arequiparescue.hospital.application.HospitalCatalogApplicationService;
import com.arequiparescue.hospital.domain.Hospital;
import com.arequiparescue.routing.domain.DijkstraPathResult;
import com.arequiparescue.routing.domain.DijkstraRouteCalculator;
import com.arequiparescue.routing.domain.NodeId;
import com.arequiparescue.routing.domain.RoadGraph;
import com.arequiparescue.routing.domain.port.RoadGraphRepository;
import com.arequiparescue.shared.presentation.AreUiFormatter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controlador MVC para la consulta de la topología del grafo de 10 nodos de Arequipa (N01 a N10),
 * sus costos abstractos enteros positivos de arista y la condición de N10 como nodo aislado sin
 * aristas activas (HU-03 CA-4, DEC-013, DEC-019, RN-RUT-006, CA-SIM-05).
 */
@Controller
public class RoadGraphTopologyController {

    private final RoadGraphRepository roadGraphRepository;
    private final AmbulanceCatalogApplicationService ambulanceCatalogService;
    private final HospitalCatalogApplicationService hospitalCatalogService;
    private final AreUiFormatter uiFormatter;
    private final DijkstraRouteCalculator dijkstraRouteCalculator = new DijkstraRouteCalculator();

    public RoadGraphTopologyController(
            RoadGraphRepository roadGraphRepository,
            AmbulanceCatalogApplicationService ambulanceCatalogService,
            HospitalCatalogApplicationService hospitalCatalogService,
            AreUiFormatter uiFormatter
    ) {
        this.roadGraphRepository = Objects.requireNonNull(roadGraphRepository);
        this.ambulanceCatalogService = Objects.requireNonNull(ambulanceCatalogService);
        this.hospitalCatalogService = Objects.requireNonNull(hospitalCatalogService);
        this.uiFormatter = Objects.requireNonNull(uiFormatter);
    }

    @GetMapping("/admin/topology")
    public String showRoadGraphTopology(
            @RequestParam(name = "origin", required = false) String origin,
            @RequestParam(name = "destination", required = false) String destination,
            Model model
    ) {
        RoadGraph graph = roadGraphRepository.loadGraph();
        Map<String, List<Ambulance>> ambulancesByNode = ambulanceCatalogService.listAmbulances().stream()
                .collect(Collectors.groupingBy(amb -> amb.currentNode().value()));
        Map<String, List<Hospital>> hospitalsByNode = hospitalCatalogService.listHospitals().stream()
                .collect(Collectors.groupingBy(hosp -> hosp.locationNode().value()));

        List<TopologyNodeView> nodeViews = graph.nodes().stream()
                .map(node -> {
                    String nodeId = node.id().value();
                    List<NodeAmbulanceView> ambViews = ambulancesByNode.getOrDefault(nodeId, List.of()).stream()
                            .map(amb -> new NodeAmbulanceView(
                                    amb.id().value(),
                                    uiFormatter.equipmentLabel(amb.equipmentType().name()),
                                    uiFormatter.ambulanceStateLabel(amb.state().name()),
                                    amb.state() == AmbulanceState.AVAILABLE
                            ))
                            .toList();
                    int availableAmbs = (int) ambViews.stream().filter(NodeAmbulanceView::available).count();
                    List<NodeHospitalView> hospViews = hospitalsByNode.getOrDefault(nodeId, List.of()).stream()
                            .map(hosp -> new NodeHospitalView(
                                    hosp.id().value(),
                                    hosp.name(),
                                    hosp.enabled(),
                                    hosp.totalCapacity(),
                                    hosp.availableSlots()
                            ))
                            .toList();
                    return new TopologyNodeView(
                            nodeId,
                            node.zoneCode(),
                            node.displayName(),
                            graph.degreeOf(node.id()),
                            graph.isIsolated(node.id()),
                            node.isolatedInBaseTopology(),
                            ambViews.size(),
                            availableAmbs,
                            ambViews.size() - availableAmbs,
                            ambViews,
                            hospViews.size(),
                            hospViews
                    );
                })
                .toList();
        List<TopologyEdgeView> edgeViews = graph.edges().stream()
                .map(edge -> new TopologyEdgeView(
                        edge.id(),
                        edge.sourceNodeId().value(),
                        edge.targetNodeId().value(),
                        edge.abstractCost()
                ))
                .toList();

        model.addAttribute("nodes", nodeViews);
        model.addAttribute("edges", edgeViews);
        model.addAttribute("nodeCount", graph.nodeCount());
        model.addAttribute("edgeCount", graph.edgeCount());

        populateRouteEvaluation(graph, origin, destination, model);
        return "admin/topology";
    }

    private void populateRouteEvaluation(RoadGraph graph, String rawOrigin, String rawDestination, Model model) {
        String normalizedOrigin = rawOrigin == null ? "" : rawOrigin.trim().toUpperCase();
        String normalizedDestination = rawDestination == null ? "" : rawDestination.trim().toUpperCase();

        model.addAttribute("routeOrigin", normalizedOrigin);
        model.addAttribute("routeDestination", normalizedDestination);
        model.addAttribute("routeEvaluated", false);
        model.addAttribute("routeFound", false);
        model.addAttribute("routeCost", null);
        model.addAttribute("routeNodes", List.of());
        model.addAttribute("routePathFormatted", "");
        model.addAttribute("routeMessage", null);

        if (normalizedOrigin.isEmpty() || normalizedDestination.isEmpty()) {
            return;
        }

        model.addAttribute("routeEvaluated", true);
        try {
            NodeId originId = NodeId.of(normalizedOrigin);
            NodeId destinationId = NodeId.of(normalizedDestination);
            if (graph.findNode(originId).isEmpty() || graph.findNode(destinationId).isEmpty()) {
                model.addAttribute("routeMessage",
                        "Uno o ambos nodos seleccionados no existen en el catálogo vigente de topología.");
                return;
            }

            Optional<DijkstraPathResult> result = dijkstraRouteCalculator.shortestPath(graph, originId, destinationId);
            if (result.isPresent()) {
                DijkstraPathResult pathResult = result.get();
                List<String> nodeIds = pathResult.path().stream().map(NodeId::value).toList();
                model.addAttribute("routeFound", true);
                model.addAttribute("routeCost", pathResult.totalCost());
                model.addAttribute("routeNodes", nodeIds);
                model.addAttribute("routePathFormatted", String.join(" \u2192 ", nodeIds));
            } else {
                model.addAttribute("routeMessage",
                        "No existe camino alcanzable entre " + originId.value() + " y " + destinationId.value()
                                + " (nodo aislado o sin conexión activa).");
            }
        } catch (IllegalArgumentException ex) {
            model.addAttribute("routeMessage", "Identificador de nodo inválido para consulta de ruta.");
        }
    }

    public record TopologyNodeView(
            String id,
            String zoneCode,
            String displayName,
            int degree,
            boolean isolated,
            boolean isolatedInBaseTopology,
            int ambulanceCount,
            int availableAmbulanceCount,
            int unavailableAmbulanceCount,
            List<NodeAmbulanceView> ambulances,
            int hospitalCount,
            List<NodeHospitalView> hospitals
    ) {
    }

    public record NodeAmbulanceView(
            String id,
            String equipmentLabel,
            String stateLabel,
            boolean available
    ) {
    }

    public record NodeHospitalView(
            String id,
            String name,
            boolean enabled,
            int totalCapacity,
            int availableSlots
    ) {
    }

    public record TopologyEdgeView(
            String id,
            String sourceNodeId,
            String targetNodeId,
            int abstractCost
    ) {
    }
}

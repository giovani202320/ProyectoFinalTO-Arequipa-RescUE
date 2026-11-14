package com.arequiparescue.routing.presentation;

import com.arequiparescue.routing.domain.GraphEdge;
import com.arequiparescue.routing.domain.GraphNode;
import com.arequiparescue.routing.domain.RoadGraph;
import com.arequiparescue.routing.domain.port.RoadGraphRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Objects;

/**
 * Controlador MVC para la consulta de la topología del grafo de 10 nodos de Arequipa (N01 a N10),
 * sus costos abstractos enteros positivos de arista y la condición de N10 como nodo aislado sin
 * aristas activas (HU-03 CA-4, DEC-013, DEC-019, RN-RUT-006, CA-SIM-05).
 */
@Controller
public class RoadGraphTopologyController {

    private final RoadGraphRepository roadGraphRepository;

    public RoadGraphTopologyController(RoadGraphRepository roadGraphRepository) {
        this.roadGraphRepository = Objects.requireNonNull(roadGraphRepository);
    }

    @GetMapping("/admin/topology")
    public String showRoadGraphTopology(Model model) {
        RoadGraph graph = roadGraphRepository.loadGraph();
        List<TopologyNodeView> nodeViews = graph.nodes().stream()
                .map(node -> new TopologyNodeView(
                        node.id().value(),
                        node.zoneCode(),
                        node.displayName(),
                        graph.degreeOf(node.id()),
                        graph.isIsolated(node.id()),
                        node.isolatedInBaseTopology()
                ))
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
        return "admin/topology";
    }

    public record TopologyNodeView(
            String id,
            String zoneCode,
            String displayName,
            int degree,
            boolean isolated,
            boolean isolatedInBaseTopology
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

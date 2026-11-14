package com.arequiparescue.routing.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Agregado de dominio puro que representa el grafo vial simplificado e inmutable de Arequipa.
 * Verifica la integridad referencial entre nodos y aristas, costos positivos y aislamiento
 * de nodos sin aristas activas como N10 (DEC-013, DEC-019, RN-RUT-006, CA-SIM-05).
 */
public final class RoadGraph {

    private final Map<NodeId, GraphNode> nodesById;
    private final List<GraphEdge> edges;
    private final Map<NodeId, List<GraphEdge>> adjacencyByNode;

    public RoadGraph(List<GraphNode> nodes, List<GraphEdge> edges) {
        Objects.requireNonNull(nodes, "La colección de nodos del grafo no puede ser nula.");
        Objects.requireNonNull(edges, "La colección de aristas del grafo no puede ser nula.");
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("El grafo debe contener al menos un nodo.");
        }

        Map<NodeId, GraphNode> nodeMap = new LinkedHashMap<>();
        Map<NodeId, List<GraphEdge>> adj = new LinkedHashMap<>();
        for (GraphNode node : nodes) {
            Objects.requireNonNull(node, "Ningún nodo del grafo puede ser nulo.");
            if (nodeMap.putIfAbsent(node.id(), node) != null) {
                throw new IllegalArgumentException("Nodo duplicado en el grafo: " + node.id());
            }
            adj.put(node.id(), new ArrayList<>());
        }

        Set<String> edgeIds = new LinkedHashSet<>();
        Set<String> undirectedPairs = new LinkedHashSet<>();
        List<GraphEdge> validatedEdges = new ArrayList<>(edges.size());
        for (GraphEdge edge : edges) {
            Objects.requireNonNull(edge, "Ninguna arista del grafo puede ser nula.");
            if (!edgeIds.add(edge.id())) {
                throw new IllegalArgumentException("Identificador de arista duplicado: " + edge.id());
            }
            GraphNode source = nodeMap.get(edge.sourceNodeId());
            GraphNode target = nodeMap.get(edge.targetNodeId());
            if (source == null || target == null) {
                throw new IllegalArgumentException(
                        "La arista " + edge.id() + " referencia un nodo inexistente en el grafo: "
                                + edge.sourceNodeId() + " - " + edge.targetNodeId());
            }
            if (source.isolatedInBaseTopology() || target.isolatedInBaseTopology()) {
                throw new IllegalArgumentException(
                        "Un nodo marcado como aislado en la topología base no puede tener aristas activas: "
                                + edge.id());
            }
            String canonicalPair = edge.sourceNodeId().compareTo(edge.targetNodeId()) < 0
                    ? edge.sourceNodeId() + "::" + edge.targetNodeId()
                    : edge.targetNodeId() + "::" + edge.sourceNodeId();
            if (!undirectedPairs.add(canonicalPair)) {
                throw new IllegalArgumentException(
                        "Conexión bidireccional duplicada entre " + edge.sourceNodeId()
                                + " y " + edge.targetNodeId());
            }

            validatedEdges.add(edge);
            adj.get(edge.sourceNodeId()).add(edge);
            adj.get(edge.targetNodeId()).add(edge);
        }

        Map<NodeId, List<GraphEdge>> immutableAdj = new LinkedHashMap<>();
        for (Map.Entry<NodeId, List<GraphEdge>> entry : adj.entrySet()) {
            immutableAdj.put(entry.getKey(), List.copyOf(entry.getValue()));
        }

        this.nodesById = Collections.unmodifiableMap(nodeMap);
        this.edges = List.copyOf(validatedEdges);
        this.adjacencyByNode = Collections.unmodifiableMap(immutableAdj);
    }

    public int nodeCount() {
        return nodesById.size();
    }

    public int edgeCount() {
        return edges.size();
    }

    public List<GraphNode> nodes() {
        return List.copyOf(nodesById.values());
    }

    public List<GraphEdge> edges() {
        return edges;
    }

    public Optional<GraphNode> findNode(NodeId nodeId) {
        Objects.requireNonNull(nodeId, "El identificador del nodo no puede ser nulo.");
        return Optional.ofNullable(nodesById.get(nodeId));
    }

    public List<GraphEdge> incidentEdgesOf(NodeId nodeId) {
        Objects.requireNonNull(nodeId, "El identificador del nodo no puede ser nulo.");
        List<GraphEdge> incident = adjacencyByNode.get(nodeId);
        if (incident == null) {
            throw new IllegalArgumentException("El nodo " + nodeId + " no existe en el grafo.");
        }
        return incident;
    }

    public int degreeOf(NodeId nodeId) {
        return incidentEdgesOf(nodeId).size();
    }

    public boolean isIsolated(NodeId nodeId) {
        return degreeOf(nodeId) == 0;
    }

    public Optional<Integer> directEdgeCostBetween(NodeId first, NodeId second) {
        Objects.requireNonNull(first, "El primer nodo no puede ser nulo.");
        Objects.requireNonNull(second, "El segundo nodo no puede ser nulo.");
        return incidentEdgesOf(first).stream()
                .filter(edge -> edge.otherEndpoint(first).equals(second))
                .map(GraphEdge::abstractCost)
                .findFirst();
    }
}

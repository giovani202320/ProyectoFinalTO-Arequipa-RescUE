package com.arequiparescue.routing.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Servicio de dominio puro que calcula rutas de costo mínimo sobre {@link RoadGraph}
 * utilizando el algoritmo clásico de Dijkstra con desempate determinista (DEC-013, RN-RUT-002, CA-SIM-05).
 * Libre de dependencias de Spring, JPA o Servlet.
 */
public class DijkstraRouteCalculator {

    private record QueueEntry(NodeId nodeId, int distance) implements Comparable<QueueEntry> {
        @Override
        public int compareTo(QueueEntry other) {
            int distCmp = Integer.compare(this.distance, other.distance);
            return distCmp != 0 ? distCmp : this.nodeId.compareTo(other.nodeId);
        }
    }

    /**
     * Calcula la ruta más corta entre {@code origin} y {@code destination}.
     *
     * @return {@link Optional#empty()} si no existe camino alcanzable (por ejemplo hacia o desde {@code N10}).
     */
    public Optional<DijkstraPathResult> shortestPath(RoadGraph graph, NodeId origin, NodeId destination) {
        Objects.requireNonNull(graph, "El grafo vial no puede ser nulo.");
        Objects.requireNonNull(origin, "El nodo origen no puede ser nulo.");
        Objects.requireNonNull(destination, "El nodo destino no puede ser nulo.");

        if (graph.findNode(origin).isEmpty()) {
            throw new IllegalArgumentException("El nodo origen " + origin + " no existe en el grafo.");
        }
        if (graph.findNode(destination).isEmpty()) {
            throw new IllegalArgumentException("El nodo destino " + destination + " no existe en el grafo.");
        }

        if (origin.equals(destination)) {
            return Optional.of(new DijkstraPathResult(origin, destination, 0, List.of(origin)));
        }

        if (graph.isIsolated(origin) || graph.isIsolated(destination)) {
            return Optional.empty();
        }

        Map<NodeId, Integer> distances = new HashMap<>();
        Map<NodeId, NodeId> predecessors = new HashMap<>();
        Set<NodeId> visited = new HashSet<>();
        PriorityQueue<QueueEntry> queue = new PriorityQueue<>();

        distances.put(origin, 0);
        queue.add(new QueueEntry(origin, 0));

        while (!queue.isEmpty()) {
            QueueEntry current = queue.poll();
            NodeId currentNode = current.nodeId();
            if (!visited.add(currentNode)) {
                continue;
            }
            if (currentNode.equals(destination)) {
                break;
            }

            List<GraphEdge> incident = new ArrayList<>(graph.incidentEdgesOf(currentNode));
            incident.sort(Comparator.comparing(edge -> edge.otherEndpoint(currentNode)));

            for (GraphEdge edge : incident) {
                NodeId neighbor = edge.otherEndpoint(currentNode);
                if (visited.contains(neighbor)) {
                    continue;
                }
                int candidateDist = Math.addExact(current.distance(), edge.abstractCost());
                Integer knownDist = distances.get(neighbor);
                if (knownDist == null || candidateDist < knownDist) {
                    distances.put(neighbor, candidateDist);
                    predecessors.put(neighbor, currentNode);
                    queue.add(new QueueEntry(neighbor, candidateDist));
                }
            }
        }

        Integer finalDistance = distances.get(destination);
        if (finalDistance == null) {
            return Optional.empty();
        }

        List<NodeId> path = new ArrayList<>();
        NodeId cursor = destination;
        while (cursor != null) {
            path.add(cursor);
            cursor = predecessors.get(cursor);
        }
        Collections.reverse(path);

        return Optional.of(new DijkstraPathResult(origin, destination, finalDistance, path));
    }
}

package com.arequiparescue.routing.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Dominio puro — DijkstraRouteCalculator sobre grafo de Arequipa (HU-05, RN-RUT-002)")
class DijkstraRouteCalculatorTest {

    private final DijkstraRouteCalculator calculator = new DijkstraRouteCalculator();

    private static RoadGraph sampleGraph() {
        return new RoadGraph(
                List.of(
                        new GraphNode(NodeId.of("N01"), "Z01", "Centro", false),
                        new GraphNode(NodeId.of("N02"), "Z02", "Cayma", false),
                        new GraphNode(NodeId.of("N03"), "Z03", "Yanahuara", false),
                        new GraphNode(NodeId.of("N04"), "Z04", "Cerro Colorado", false),
                        new GraphNode(NodeId.of("N10"), "Z10", "Zona Aislada", true)
                ),
                List.of(
                        new GraphEdge("EDG-01", NodeId.of("N01"), NodeId.of("N02"), 10),
                        new GraphEdge("EDG-02", NodeId.of("N01"), NodeId.of("N03"), 2),
                        new GraphEdge("EDG-03", NodeId.of("N03"), NodeId.of("N02"), 3),
                        new GraphEdge("EDG-04", NodeId.of("N02"), NodeId.of("N04"), 4)
                )
        );
    }

    @Test
    @DisplayName("Calcula la ruta de costo mínimo prefiriendo el desvío más barato sobre la arista directa costosa")
    void shouldComputeMinimumCostPath() {
        Optional<DijkstraPathResult> result =
                calculator.shortestPath(sampleGraph(), NodeId.of("N01"), NodeId.of("N04"));

        assertThat(result).isPresent();
        assertThat(result.get().totalCost()).isEqualTo(9); // N01 -> N03 (2) -> N02 (3) -> N04 (4)
        assertThat(result.get().path())
                .containsExactly(NodeId.of("N01"), NodeId.of("N03"), NodeId.of("N02"), NodeId.of("N04"));
    }

    @Test
    @DisplayName("Retorna costo 0 cuando origen y destino coinciden")
    void shouldReturnZeroCostWhenOriginEqualsDestination() {
        Optional<DijkstraPathResult> result =
                calculator.shortestPath(sampleGraph(), NodeId.of("N02"), NodeId.of("N02"));

        assertThat(result).isPresent();
        assertThat(result.get().totalCost()).isZero();
        assertThat(result.get().path()).containsExactly(NodeId.of("N02"));
    }

    @Test
    @DisplayName("Retorna vacío cuando origen o destino es el nodo aislado N10 (CA-SIM-05)")
    void shouldReturnEmptyForDisconnectedNodeN10() {
        assertThat(calculator.shortestPath(sampleGraph(), NodeId.of("N01"), NodeId.of("N10"))).isEmpty();
        assertThat(calculator.shortestPath(sampleGraph(), NodeId.of("N10"), NodeId.of("N01"))).isEmpty();
    }
}

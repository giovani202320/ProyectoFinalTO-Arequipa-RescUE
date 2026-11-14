package com.arequiparescue.routing.domain;

import com.arequiparescue.routing.infrastructure.persistence.GraphEdgeJpaEntity;
import com.arequiparescue.routing.infrastructure.persistence.GraphNodeJpaEntity;
import com.arequiparescue.routing.infrastructure.persistence.RoadGraphPersistenceMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Semilla del grafo de Arequipa (V2__seed_initial_graph.sql) y modelo RoadGraph (DEC-019, RN-RUT-006, CA-SIM-05)")
class ArequipaSeedGraphTest {

    private static final Path SEED_SQL_PATH = Path.of("src/main/resources/db/migration/V2__seed_initial_graph.sql");

    private static final Pattern NODE_ROW_PATTERN = Pattern.compile(
            "\\('(N\\d{2})',\\s*'([^']+)',\\s*'([^']+)',\\s*(TRUE|FALSE)\\)"
    );

    private static final Pattern EDGE_ROW_PATTERN = Pattern.compile(
            "\\('(EDG-\\d{2})',\\s*'(N\\d{2})',\\s*'(N\\d{2})',\\s*(-?\\d+)\\)"
    );

    @Test
    @DisplayName("Verifica que la migración semilla V2 define exactamente 10 nodos, 12 aristas aprobadas, pesos positivos y N10 aislado")
    void shouldVerifyNormativeGraphSeedIntegrity() throws IOException {
        String sql = Files.readString(SEED_SQL_PATH);

        List<GraphNodeJpaEntity> nodeEntities = new ArrayList<>();
        Matcher nodeMatcher = NODE_ROW_PATTERN.matcher(sql);
        while (nodeMatcher.find()) {
            nodeEntities.add(new GraphNodeJpaEntity(
                    nodeMatcher.group(1),
                    nodeMatcher.group(2),
                    nodeMatcher.group(3),
                    Boolean.parseBoolean(nodeMatcher.group(4))
            ));
        }

        List<GraphEdgeJpaEntity> edgeEntities = new ArrayList<>();
        Matcher edgeMatcher = EDGE_ROW_PATTERN.matcher(sql);
        while (edgeMatcher.find()) {
            edgeEntities.add(new GraphEdgeJpaEntity(
                    edgeMatcher.group(1),
                    edgeMatcher.group(2),
                    edgeMatcher.group(3),
                    Integer.parseInt(edgeMatcher.group(4))
            ));
        }

        RoadGraphPersistenceMapper mapper = new RoadGraphPersistenceMapper();
        RoadGraph graph = mapper.toDomainGraph(nodeEntities, edgeEntities);

        // 1. Exactamente 10 nodos (N01 a N10)
        assertThat(graph.nodeCount()).isEqualTo(10);
        assertThat(graph.nodes())
                .extracting(node -> node.id().value())
                .containsExactly("N01", "N02", "N03", "N04", "N05", "N06", "N07", "N08", "N09", "N10");

        // 2. Exactamente 12 aristas aprobadas con pesos enteros estrictamente positivos
        assertThat(graph.edgeCount()).isEqualTo(12);
        assertThat(graph.edges()).allSatisfy(edge -> assertThat(edge.abstractCost()).isPositive());

        // 3. Verificación exacta de las 12 conexiones y costos de RN-RUT-006
        assertThat(graph.directEdgeCostBetween(NodeId.of("N01"), NodeId.of("N02"))).contains(2);
        assertThat(graph.directEdgeCostBetween(NodeId.of("N01"), NodeId.of("N05"))).contains(2);
        assertThat(graph.directEdgeCostBetween(NodeId.of("N01"), NodeId.of("N07"))).contains(3);
        assertThat(graph.directEdgeCostBetween(NodeId.of("N01"), NodeId.of("N09"))).contains(4);
        assertThat(graph.directEdgeCostBetween(NodeId.of("N02"), NodeId.of("N03"))).contains(3);
        assertThat(graph.directEdgeCostBetween(NodeId.of("N02"), NodeId.of("N09"))).contains(2);
        assertThat(graph.directEdgeCostBetween(NodeId.of("N03"), NodeId.of("N04"))).contains(2);
        assertThat(graph.directEdgeCostBetween(NodeId.of("N03"), NodeId.of("N07"))).contains(2);
        assertThat(graph.directEdgeCostBetween(NodeId.of("N04"), NodeId.of("N09"))).contains(4);
        assertThat(graph.directEdgeCostBetween(NodeId.of("N05"), NodeId.of("N06"))).contains(3);
        assertThat(graph.directEdgeCostBetween(NodeId.of("N05"), NodeId.of("N08"))).contains(3);
        assertThat(graph.directEdgeCostBetween(NodeId.of("N06"), NodeId.of("N07"))).contains(2);

        // 4. N10 (YURA_AISLADO) está en el catálogo pero no tiene ninguna arista activa
        NodeId n10 = NodeId.of("N10");
        assertThat(graph.findNode(n10)).isPresent();
        assertThat(graph.findNode(n10).orElseThrow().zoneCode()).isEqualTo("YURA_AISLADO");
        assertThat(graph.findNode(n10).orElseThrow().isolatedInBaseTopology()).isTrue();
        assertThat(graph.isIsolated(n10)).isTrue();
        assertThat(graph.degreeOf(n10)).isZero();

        // Todos los nodos N01..N09 tienen al menos una arista activa
        for (int i = 1; i <= 9; i++) {
            NodeId connectedNode = NodeId.of(String.format("N%02d", i));
            assertThat(graph.isIsolated(connectedNode)).isFalse();
        }
    }

    @Test
    @DisplayName("Rechaza aristas con peso no positivo o que conecten al nodo aislado N10")
    void shouldRejectInvalidEdgeWeightOrEdgeConnectedToIsolatedNode() {
        assertThatThrownBy(() -> new GraphEdge("EDG-BAD", NodeId.of("N01"), NodeId.of("N02"), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("> 0");

        GraphNode n01 = new GraphNode(NodeId.of("N01"), "CERCADO", "Cercado", false);
        GraphNode n10 = new GraphNode(NodeId.of("N10"), "YURA_AISLADO", "Yura Aislado", true);
        GraphEdge forbiddenEdge = new GraphEdge("EDG-99", NodeId.of("N01"), NodeId.of("N10"), 5);

        assertThatThrownBy(() -> new RoadGraph(List.of(n01, n10), List.of(forbiddenEdge)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aislado");
    }
}

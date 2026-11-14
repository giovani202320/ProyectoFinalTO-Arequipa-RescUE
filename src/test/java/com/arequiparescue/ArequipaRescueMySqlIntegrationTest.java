package com.arequiparescue;

import com.arequiparescue.routing.domain.NodeId;
import com.arequiparescue.routing.domain.RoadGraph;
import com.arequiparescue.routing.domain.port.RoadGraphRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de integración contra una instancia activa de MySQL 8.4 LTS con Flyway y Hibernate validate.
 * Se habilita con {@code -Dare.mysql.integration=true} cuando el servidor MySQL está disponible.
 */
@SpringBootTest
@Tag("mysql-integration")
@EnabledIfSystemProperty(named = "are.mysql.integration", matches = "true")
@DisplayName("Integración MySQL 8.4 + Flyway V1/V2 + RoadGraphJpaAdapter")
class ArequipaRescueMySqlIntegrationTest {

    @Autowired
    private RoadGraphRepository roadGraphRepository;

    @Test
    @DisplayName("Ejecuta migraciones Flyway V1 y V2 en MySQL 8.4 y carga el grafo de 10 nodos y 12 aristas")
    void shouldLoadSeededGraphFromMySql() {
        RoadGraph graph = roadGraphRepository.loadGraph();

        assertThat(graph.nodeCount()).isEqualTo(10);
        assertThat(graph.edgeCount()).isEqualTo(12);
        assertThat(graph.edges()).allSatisfy(edge -> assertThat(edge.abstractCost()).isPositive());
        assertThat(graph.isIsolated(NodeId.of("N10"))).isTrue();
    }
}

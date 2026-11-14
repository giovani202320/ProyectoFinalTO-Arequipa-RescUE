package com.arequiparescue;

import com.arequiparescue.routing.infrastructure.persistence.SpringDataGraphEdgeRepository;
import com.arequiparescue.routing.infrastructure.persistence.SpringDataGraphNodeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class,
        FlywayAutoConfiguration.class
})
@DisplayName("ArequipaRescueApplication — Arranque del contexto Spring Boot y petición HTTP GET /")
class ArequipaRescueApplicationSmokeTest {

    @MockitoBean
    private SpringDataGraphNodeRepository graphNodeRepository;

    @MockitoBean
    private SpringDataGraphEdgeRepository graphEdgeRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Inicia el contenedor embebido de Spring Boot y sirve GET / con estado 200 OK")
    void shouldStartApplicationContextAndServeRootEndpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("ARE — Arequipa RescUE")
                .contains("Simulador académico")
                .contains("Incremento 01");
    }
}

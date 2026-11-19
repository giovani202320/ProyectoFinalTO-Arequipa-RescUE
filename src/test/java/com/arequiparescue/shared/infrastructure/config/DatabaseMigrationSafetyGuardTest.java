package com.arequiparescue.shared.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DatabaseMigrationSafetyGuard — Protección fail-closed de conexión JDBC y Flyway")
class DatabaseMigrationSafetyGuardTest {

    @Test
    @DisplayName("Caso A: rechaza arranque sin ARE_DB_URL fuera de perfiles demo/mysql-test")
    void shouldRejectStartupWithoutExplicitDatasourceUrl() {
        assertThatThrownBy(() -> DatabaseMigrationSafetyGuard.validatePreConnection(
                Set.of(), "", true, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ARE_DB_URL");
    }

    @Test
    @DisplayName("Caso B y C: permite perfiles demo y mysql-test sin ARE_DB_URL porque definen su propio destino aislado")
    void shouldAllowDemoAndMysqlTestProfilesWithoutGenericAreDbUrl() {
        assertThatCode(() -> DatabaseMigrationSafetyGuard.validatePreConnection(
                Set.of("demo"), "", true, false))
                .doesNotThrowAnyException();

        assertThatCode(() -> DatabaseMigrationSafetyGuard.validatePreConnection(
                Set.of("mysql-test"), "", true, false))
                .doesNotThrowAnyException();

        assertThatCode(() -> DatabaseMigrationSafetyGuard.validateCatalog(
                "arequipa_rescue_demo", Set.of("demo"), true, false))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Caso D: el perfil historical con Flyway deshabilitado permite operar sobre arequipa_rescue sin migrar")
    void shouldAllowHistoricalProfileWithFlywayDisabledOnHistoricalDatabase() {
        assertThatCode(() -> DatabaseMigrationSafetyGuard.validatePreConnection(
                Set.of("historical"),
                "jdbc:mysql://localhost:3306/arequipa_rescue",
                false,
                false))
                .doesNotThrowAnyException();

        assertThatCode(() -> DatabaseMigrationSafetyGuard.validateCatalog(
                "arequipa_rescue", Set.of("historical"), false, false))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Caso E: rechaza configuraciones contradictorias y migraciones automáticas no autorizadas sobre arequipa_rescue")
    void shouldRejectContradictoryConfigurationsAndUnprotectedHistoricalAccess() {
        assertThatThrownBy(() -> DatabaseMigrationSafetyGuard.validatePreConnection(
                Set.of("historical"),
                "jdbc:mysql://localhost:3306/arequipa_rescue",
                true,
                false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spring.flyway.enabled=false");

        assertThatThrownBy(() -> DatabaseMigrationSafetyGuard.validatePreConnection(
                Set.of("historical"),
                "jdbc:mysql://localhost:3306/arequipa_rescue",
                false,
                true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ARE_ALLOW_HISTORICAL_MIGRATION");

        assertThatThrownBy(() -> DatabaseMigrationSafetyGuard.validatePreConnection(
                Set.of("historical", "demo"),
                "jdbc:mysql://localhost:3306/arequipa_rescue_demo",
                false,
                false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no puede combinarse");

        assertThatThrownBy(() -> DatabaseMigrationSafetyGuard.validateCatalog(
                "arequipa_rescue", Set.of(), true, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("protegida contra migraciones automáticas");
    }

    @Test
    @DisplayName("Caso F: permite migración explícitamente autorizada con ARE_ALLOW_HISTORICAL_MIGRATION=true sin perfil historical")
    void shouldAllowExplicitlyAuthorizedMigrationOnHistoricalDatabase() {
        assertThatCode(() -> DatabaseMigrationSafetyGuard.validateCatalog(
                "arequipa_rescue", Set.of(), true, true))
                .doesNotThrowAnyException();
    }
}

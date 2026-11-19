package com.arequiparescue.shared.infrastructure.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Guardia fail-closed que se ejecuta sobre {@link DataSource} antes de la inicialización de Flyway
 * e Hibernate para impedir:
 * <ol>
 *   <li>Arranques sin {@code ARE_DB_URL} explícita fuera de perfiles con destino dedicado ({@code demo}, {@code mysql-test}).</li>
 *   <li>Migraciones automáticas no autorizadas sobre la base histórica {@code arequipa_rescue}.</li>
 *   <li>Configuraciones contradictorias que combinen el perfil {@code historical} con Flyway habilitado o con otros perfiles de entorno.</li>
 * </ol>
 */
@Component
public class DatabaseMigrationSafetyGuard implements BeanPostProcessor {

    static final String HISTORICAL_DATABASE = "arequipa_rescue";
    static final String HISTORICAL_PROFILE = "historical";
    static final String DEMO_PROFILE = "demo";
    static final String MYSQL_TEST_PROFILE = "mysql-test";

    private final Environment environment;

    public DatabaseMigrationSafetyGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource dataSource) {
            Set<String> activeProfiles = Arrays.stream(environment.getActiveProfiles())
                    .map(String::trim)
                    .filter(p -> !p.isEmpty())
                    .collect(Collectors.toSet());

            String datasourceUrl = environment.getProperty("spring.datasource.url", "");
            boolean flywayEnabled = environment.getProperty("spring.flyway.enabled", Boolean.class, true);
            boolean explicitHistoricalMigration = Boolean.parseBoolean(
                    environment.getProperty("ARE_ALLOW_HISTORICAL_MIGRATION", "false"));

            validatePreConnection(activeProfiles, datasourceUrl, flywayEnabled, explicitHistoricalMigration);

            try (Connection connection = dataSource.getConnection()) {
                validateCatalog(connection.getCatalog(), activeProfiles, flywayEnabled, explicitHistoricalMigration);
            } catch (SQLException ex) {
                throw new IllegalStateException(
                        "ABORTADO (fail-closed): no se pudo verificar la base de datos destino antes de Flyway.", ex);
            }
        }
        return bean;
    }

    static void validatePreConnection(
            Set<String> activeProfiles,
            String datasourceUrl,
            boolean flywayEnabled,
            boolean explicitHistoricalMigration
    ) {
        boolean isHistorical = activeProfiles.contains(HISTORICAL_PROFILE);
        boolean isDemo = activeProfiles.contains(DEMO_PROFILE);
        boolean isMysqlTest = activeProfiles.contains(MYSQL_TEST_PROFILE);

        if (isHistorical && (isDemo || isMysqlTest)) {
            throw new IllegalStateException(
                    "ABORTADO (fail-closed): el perfil 'historical' no puede combinarse con 'demo' ni 'mysql-test'.");
        }
        if (isHistorical && flywayEnabled) {
            throw new IllegalStateException(
                    "ABORTADO (fail-closed): configuración contradictoria; el perfil 'historical' exige spring.flyway.enabled=false.");
        }
        if (isHistorical && explicitHistoricalMigration) {
            throw new IllegalStateException(
                    "ABORTADO (fail-closed): configuración contradictoria; no active 'ARE_ALLOW_HISTORICAL_MIGRATION=true' junto con el perfil 'historical'.");
        }

        if (!isDemo && !isMysqlTest && (datasourceUrl == null || datasourceUrl.isBlank())) {
            throw new IllegalStateException(
                    "ABORTADO (fail-closed): debe definir ARE_DB_URL explícitamente en el entorno "
                            + "(por ejemplo jdbc:mysql://localhost:3306/arequipa_rescue_demo?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC) "
                            + "o activar el perfil 'demo' / 'mysql-test'.");
        }
    }

    static void validateCatalog(
            String catalog,
            Set<String> activeProfiles,
            boolean flywayEnabled,
            boolean explicitHistoricalMigration
    ) {
        if (catalog == null || catalog.isBlank()) {
            throw new IllegalStateException(
                    "ABORTADO (fail-closed): no se pudo determinar el catálogo JDBC de destino.");
        }

        boolean isHistoricalDatabase = HISTORICAL_DATABASE.equalsIgnoreCase(catalog.trim());
        boolean isHistoricalProfile = activeProfiles.contains(HISTORICAL_PROFILE);

        if (isHistoricalDatabase && flywayEnabled && !explicitHistoricalMigration) {
            throw new IllegalStateException(
                    "ABORTADO (fail-closed): la base histórica '" + HISTORICAL_DATABASE
                            + "' está protegida contra migraciones automáticas de Flyway. "
                            + "Para operarla normalmente use '--spring.profiles.active=historical' con ARE_DB_URL explícita. "
                            + "Para aplicar una migración autorizada y respaldada, defina ARE_ALLOW_HISTORICAL_MIGRATION=true.");
        }

        if (isHistoricalDatabase && !isHistoricalProfile && !explicitHistoricalMigration) {
            throw new IllegalStateException(
                    "ABORTADO (fail-closed): para conectarse a '" + HISTORICAL_DATABASE
                            + "' debe usar el perfil 'historical' o autorizar migración explícitamente.");
        }
    }
}

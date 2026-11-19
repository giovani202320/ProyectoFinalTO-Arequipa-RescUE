package com.arequiparescue;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Guardia fail-closed para el perfil {@code mysql-test}: intercepta el bean {@link DataSource}
 * mediante {@link BeanPostProcessor} antes de que se inicialicen Flyway, Hibernate o cualquier prueba,
 * impidiendo escrituras accidentales contra {@code arequipa_rescue} o {@code arequipa_rescue_demo}.
 */
@Component
@Profile("mysql-test")
class MySqlTestDatabaseSafetyGuard implements BeanPostProcessor {

    static final String REQUIRED_TEST_DATABASE = "arequipa_rescue_test";

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource dataSource) {
            try (Connection connection = dataSource.getConnection()) {
                validateDatabaseName(connection.getCatalog());
            } catch (SQLException ex) {
                throw new IllegalStateException(
                        "ABORTADO (fail-closed): no se pudo verificar la base de datos de pruebas.", ex);
            }
        }
        return bean;
    }

    static void validateDatabaseName(String catalog) {
        if (catalog == null || !REQUIRED_TEST_DATABASE.equalsIgnoreCase(catalog.trim())) {
            throw new IllegalStateException(
                    "ABORTADO (fail-closed): el perfil mysql-test solo puede ejecutarse contra '"
                            + REQUIRED_TEST_DATABASE + "', pero se conectó a '" + catalog + "'."
            );
        }
    }
}

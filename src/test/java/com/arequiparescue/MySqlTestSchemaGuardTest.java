package com.arequiparescue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MySqlTestSchemaGuard — Protección contra limpieza fuera del esquema de pruebas")
class MySqlTestSchemaGuardTest {

    @Test
    @DisplayName("Permite continuar únicamente cuando el esquema activo es arequipa_rescue_test")
    void shouldAllowIsolatedTestSchema() {
        assertThatCode(() -> MySqlTestSchemaGuard.assertIsolatedTestSchemaName("arequipa_rescue_test"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Falla inmediatamente cuando el esquema activo es arequipa_rescue u otro no aislado")
    void shouldRejectDevelopmentOrUnknownSchema() {
        assertThatThrownBy(() -> MySqlTestSchemaGuard.assertIsolatedTestSchemaName("arequipa_rescue"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("arequipa_rescue_test")
                .hasMessageContaining("arequipa_rescue");

        assertThatThrownBy(() -> MySqlTestSchemaGuard.assertIsolatedTestSchemaName(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("arequipa_rescue_test");
    }
}

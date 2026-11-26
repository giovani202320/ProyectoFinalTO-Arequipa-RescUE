package com.arequiparescue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MySqlTestDatabaseSafetyGuard — Protección fail-closed de base de pruebas")
class MySqlTestDatabaseSafetyGuardTest {

    @Test
    @DisplayName("Acepta únicamente arequipa_rescue_test y rechaza arequipa_rescue o arequipa_rescue_demo")
    void shouldAllowOnlyIsolatedTestDatabase() {
        assertThatCode(() -> MySqlTestDatabaseSafetyGuard.validateDatabaseName("arequipa_rescue_test"))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> MySqlTestDatabaseSafetyGuard.validateDatabaseName("arequipa_rescue"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("fail-closed");

        assertThatThrownBy(() -> MySqlTestDatabaseSafetyGuard.validateDatabaseName("arequipa_rescue_demo"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("fail-closed");

        assertThatThrownBy(() -> MySqlTestDatabaseSafetyGuard.validateDatabaseName(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("fail-closed");
    }
}

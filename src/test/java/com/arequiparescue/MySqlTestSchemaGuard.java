package com.arequiparescue;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Runtime guard for MySQL integration tests.
 * Prevents destructive test cleanup from executing outside the isolated {@code arequipa_rescue_test} schema.
 */
final class MySqlTestSchemaGuard {

    static final String EXPECTED_TEST_SCHEMA = "arequipa_rescue_test";

    private MySqlTestSchemaGuard() {
    }

    static void assertIsolatedTestSchema(JdbcTemplate jdbcTemplate) {
        if (jdbcTemplate == null) {
            throw new IllegalStateException("JdbcTemplate is required to verify the active MySQL test schema.");
        }
        String activeSchema = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        assertIsolatedTestSchemaName(activeSchema);
    }

    static void assertIsolatedTestSchemaName(String activeSchema) {
        if (!EXPECTED_TEST_SCHEMA.equals(activeSchema)) {
            throw new IllegalStateException(
                    "Unsafe test schema detected: expected '" + EXPECTED_TEST_SCHEMA
                            + "', but active schema is '" + activeSchema + "'."
            );
        }
    }
}

package com.arequiparescue.shared.infrastructure;

import com.arequiparescue.shared.domain.LogicalClock;
import com.arequiparescue.shared.domain.LogicalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Hidrata de forma monotónica el {@link LogicalClock} en memoria al arrancar la aplicación
 * (después de que Flyway e Hibernate validen el esquema) leyendo únicamente el máximo tiempo lógico
 * persistido en las tablas operacionales y de escenarios, sin modificar ningún registro histórico.
 */
@Component
public class LogicalClockStartupSynchronizer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LogicalClockStartupSynchronizer.class);

    static final String MAX_PERSISTED_LOGICAL_TICKS_SQL = """
            SELECT COALESCE(MAX(max_t), 0) FROM (
                SELECT MAX(current_logical_t) AS max_t FROM simulation_scenarios
                UNION ALL
                SELECT MAX(GREATEST(
                    registered_at_t,
                    COALESCE(queue_entered_at_t, 0),
                    COALESCE(assigned_at_t, 0),
                    COALESCE(at_incident_at_t, 0),
                    COALESCE(hospital_queue_entered_at_t, 0),
                    COALESCE(in_transport_at_t, 0),
                    COALESCE(completed_at_t, 0)
                )) AS max_t FROM emergencies
                UNION ALL
                SELECT MAX(GREATEST(
                    reserved_at_t,
                    COALESCE(occupied_at_t, 0),
                    COALESCE(discharged_at_t, 0)
                )) AS max_t FROM hospital_reservations
            ) persisted_logical_times
            """;

    private final LogicalClock logicalClock;
    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    public LogicalClockStartupSynchronizer(
            LogicalClock logicalClock,
            ObjectProvider<JdbcTemplate> jdbcTemplateProvider
    ) {
        this.logicalClock = Objects.requireNonNull(logicalClock);
        this.jdbcTemplateProvider = Objects.requireNonNull(jdbcTemplateProvider);
    }

    @Override
    public void run(ApplicationArguments args) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null) {
            return;
        }
        LogicalTime hydrated = synchronizeFromDatabase(logicalClock, jdbcTemplate);
        log.info("Reloj lógico global sincronizado al arranque en t={}.", hydrated.ticks());
    }

    public static LogicalTime synchronizeFromDatabase(LogicalClock clock, JdbcTemplate jdbcTemplate) {
        Objects.requireNonNull(clock, "El reloj lógico es obligatorio.");
        Objects.requireNonNull(jdbcTemplate, "JdbcTemplate es obligatorio.");
        Long maxPersisted = jdbcTemplate.queryForObject(MAX_PERSISTED_LOGICAL_TICKS_SQL, Long.class);
        long safeMax = (maxPersisted != null && maxPersisted > 0L) ? maxPersisted : 0L;
        return clock.synchronizeAtLeast(LogicalTime.of(safeMax));
    }
}

package com.arequiparescue.shared.infrastructure;

import com.arequiparescue.shared.domain.LogicalClock;
import com.arequiparescue.shared.domain.LogicalTime;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de infraestructura para exponer el reloj lógico monotónico ({@code t >= 0})
 * sin contaminar la clase de dominio {@link LogicalClock} con anotaciones de Spring (DEC-003, ADR-0001).
 */
@Configuration
public class SimulationClockConfig {

    @Bean
    public LogicalClock logicalClock() {
        return new LogicalClock(LogicalTime.ZERO);
    }
}

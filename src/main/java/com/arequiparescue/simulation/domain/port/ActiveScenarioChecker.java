package com.arequiparescue.simulation.domain.port;

/**
 * Puerto de dominio para verificar si existe un escenario experimental en estado {@code ACTIVE},
 * requerido para aplicar las restricciones de edición y publicación durante escenarios activos
 * (RN-EME-012, RN-HOS-015, CA-HOS-12, HU-03, HU-04, ADR-0002 Secc. 4.7).
 */
public interface ActiveScenarioChecker {

    boolean isAnyScenarioActive();

    /**
     * Verifica la existencia de escenarios {@code ACTIVE} adquiriendo bloqueo pesimista
     * {@code SELECT ... FOR UPDATE} como primer paso del orden canónico de ADR-0002.
     */
    boolean isAnyScenarioActiveForUpdate();
}

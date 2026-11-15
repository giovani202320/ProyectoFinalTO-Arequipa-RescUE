package com.arequiparescue.reporting.domain.port;

import com.arequiparescue.reporting.domain.ReportingSourceData;

/**
 * Puerto de consulta de solo lectura para obtener los datos persistidos necesarios
 * para construir las 7 métricas ({@code RN-MET-004}) y el registro experimental ({@code RN-MET-003}).
 */
public interface ReportingDataQueryPort {

    ReportingSourceData loadSourceData(String requestedScenarioId, long fallbackLogicalTicks);
}

package com.arequiparescue.reporting.application;

import com.arequiparescue.reporting.domain.ReportingCalculator;
import com.arequiparescue.reporting.domain.ReportingSourceData;
import com.arequiparescue.reporting.domain.ReportingSupplementaryMetrics;
import com.arequiparescue.reporting.domain.SimulationReportSnapshot;
import com.arequiparescue.reporting.domain.port.ReportingDataQueryPort;
import com.arequiparescue.shared.domain.LogicalClock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Servicio de aplicación de solo lectura para consultar el catálogo de 7 métricas experimentales
 * ({@code RN-MET-004}) y el registro funcional trazable ({@code RN-MET-003}) en {@code HU-14}.
 */
@Service
public class ReportingApplicationService {

    private final ReportingDataQueryPort queryPort;
    private final LogicalClock logicalClock;

    public ReportingApplicationService(
            ReportingDataQueryPort queryPort,
            LogicalClock logicalClock
    ) {
        this.queryPort = Objects.requireNonNull(queryPort);
        this.logicalClock = Objects.requireNonNull(logicalClock);
    }

    @Transactional(readOnly = true)
    public ReportViewResult generateReport(String scenarioId, String emergencyStateFilter) {
        return generateFilteredReport(
                scenarioId,
                new ReportingSupplementaryMetrics.TraceabilityFilter(emergencyStateFilter, null, null, null));
    }

    @Transactional(readOnly = true)
    public ReportViewResult generateFilteredReport(
            String scenarioId,
            ReportingSupplementaryMetrics.TraceabilityFilter filter
    ) {
        ReportingSourceData sourceData = queryPort.loadSourceData(scenarioId, logicalClock.currentTicks());
        SimulationReportSnapshot snapshot = ReportingCalculator.calculateWithFilter(sourceData, filter);
        return new ReportViewResult(snapshot, sourceData.scenarios());
    }

    public record ReportViewResult(
            SimulationReportSnapshot snapshot,
            List<ReportingSourceData.ScenarioRow> scenarios
    ) {
    }
}

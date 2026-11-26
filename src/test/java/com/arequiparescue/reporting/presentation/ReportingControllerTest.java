package com.arequiparescue.reporting.presentation;

import com.arequiparescue.reporting.application.ReportingApplicationService;
import com.arequiparescue.reporting.domain.ReportingCalculator;
import com.arequiparescue.reporting.domain.ReportingSourceData;
import com.arequiparescue.reporting.domain.ReportingSupplementaryMetrics;
import com.arequiparescue.reporting.domain.SimulationReportSnapshot;
import com.arequiparescue.shared.presentation.AreUiFormatter;
import com.arequiparescue.shared.presentation.HtmlStructureAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportingController.class)
@Import(AreUiFormatter.class)
class ReportingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportingApplicationService reportingService;

    @Test
    void rendersAllSevenMetricsAndEmptyStates() throws Exception {
        ReportingSourceData source = new ReportingSourceData(
                "SCN-01",
                "ACTIVE",
                0L,
                List.of(new ReportingSourceData.ScenarioRow("SCN-01", "Base", "ACTIVE", 0L)),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        SimulationReportSnapshot snapshot = ReportingCalculator.calculate(source, null);
        when(reportingService.generateReport(null, null))
                .thenReturn(new ReportingApplicationService.ReportViewResult(snapshot, source.scenarios()));

        String html = mockMvc.perform(get("/reporting"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("MET-01")))
                .andExpect(content().string(containsString("MET-02")))
                .andExpect(content().string(containsString("MET-03")))
                .andExpect(content().string(containsString("MET-04")))
                .andExpect(content().string(containsString("MET-05")))
                .andExpect(content().string(containsString("MET-06")))
                .andExpect(content().string(containsString("MET-07")))
                .andExpect(content().string(containsString("Sin mediciones cerradas")))
                .andExpect(content().string(containsString("Registro funcional experimental trazable")))
                .andExpect(content().string(containsString("Esperando ambulancia")))
                .andExpect(content().string(containsString("/vendor/chartjs/4.4.7/chart.umd.min.js")))
                .andExpect(content().string(containsString("/js/are-reporting-charts.js")))
                .andExpect(content().string(containsString("/css/are-design-system.css")))
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        HtmlStructureAssertions.assertSingleAppShellStructure(html, "reporting-page-title");
        assertThat(HtmlStructureAssertions.countOccurrences(html, "<form")).isEqualTo(1);
    }

    @Test
    void forwardsScenarioAndStateFiltersToService() throws Exception {
        ReportingSourceData source = new ReportingSourceData(
                "SCN-02",
                "COMPLETED",
                10L,
                List.of(new ReportingSourceData.ScenarioRow("SCN-02", "Cierre", "COMPLETED", 10L)),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        SimulationReportSnapshot snapshot = ReportingCalculator.calculate(source, "WAITING_AMBULANCE");
        when(reportingService.generateReport("SCN-02", "WAITING_AMBULANCE"))
                .thenReturn(new ReportingApplicationService.ReportViewResult(snapshot, source.scenarios()));

        mockMvc.perform(get("/reporting")
                        .param("scenarioId", "SCN-02")
                        .param("state", "WAITING_AMBULANCE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("SCN-02")));

        verify(reportingService).generateReport("SCN-02", "WAITING_AMBULANCE");
    }

    @Test
    void rendersPopulatedMetricsChartsDataAndSpanishLabels() throws Exception {
        ReportingSourceData source = new ReportingSourceData(
                "SCN-03",
                "ACTIVE",
                10L,
                List.of(new ReportingSourceData.ScenarioRow("SCN-03", "Prueba", "ACTIVE", 10L)),
                List.of(new ReportingSourceData.EmergencyRow(
                        "EMG-00001", "COMPLETED", "RUL-V01", "AMB-01", "HOSP-01",
                        null, 0L, 1L, 3L, 5L, 5L, 6L, 9L)),
                List.of(new ReportingSourceData.AmbulanceRow("AMB-01", "BASIC", "AVAILABLE", null)),
                List.of(),
                List.of(new ReportingSourceData.OperationResultRow(
                        "OP-1", "AUTOMATIC_DISPATCH", "EMG-00001", "AMB-01",
                        1, "ASSIGNED", "Asignación confirmada", 3L)),
                List.of()
        );
        SimulationReportSnapshot snapshot = ReportingCalculator.calculate(source, null);
        when(reportingService.generateReport("SCN-03", null))
                .thenReturn(new ReportingApplicationService.ReportViewResult(snapshot, source.scenarios()));

        mockMvc.perform(get("/reporting").param("scenarioId", "SCN-03"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("unidades de tiempo lógico")))
                .andExpect(content().string(containsString("60.00%")))
                .andExpect(content().string(containsString("Básica")))
                .andExpect(content().string(containsString("Disponible")))
                .andExpect(content().string(containsString("Atención finalizada")))
                .andExpect(content().string(containsString("Despacho automático")))
                .andExpect(content().string(containsString("Asignada")))
                .andExpect(content().string(containsString("Datos globales")))
                .andExpect(content().string(containsString("Referencia temporal t")))
                .andExpect(content().string(containsString("EMG-00001")))
                .andExpect(content().string(containsString("AMB-01")))
                .andExpect(content().string(containsString("HOSP-01")))
                .andExpect(content().string(containsString("RUL-V01")))
                .andExpect(content().string(containsString("title=\"AUTOMATIC_DISPATCH\"")))
                .andExpect(content().string(containsString("title=\"ASSIGNED\"")))
                .andExpect(content().string(not(containsString(">ASSIGNED<"))))
                .andExpect(content().string(containsString("data-met01=\"2.0\"")))
                .andExpect(content().string(containsString("data-percent=\"60.0\"")));
    }

    @Test
    void updatesMet05WhenReferenceScenarioTimeChangesWhileKeepingGlobalMetricsInvariant() throws Exception {
        ReportingSourceData baseSourceAt10 = new ReportingSourceData(
                "SCN-10",
                "ACTIVE",
                10L,
                List.of(
                        new ReportingSourceData.ScenarioRow("SCN-10", "Escenario 10", "ACTIVE", 10L),
                        new ReportingSourceData.ScenarioRow("SCN-20", "Escenario 20", "COMPLETED", 20L)
                ),
                List.of(new ReportingSourceData.EmergencyRow(
                        "EMG-00001", "COMPLETED", "RUL-V01", "AMB-01", "HOSP-01",
                        null, 0L, 1L, 3L, 5L, 5L, 6L, 9L)),
                List.of(new ReportingSourceData.AmbulanceRow("AMB-01", "BASIC", "AVAILABLE", null)),
                List.of(),
                List.of(),
                List.of()
        );
        ReportingSourceData sameGlobalAt20 = new ReportingSourceData(
                "SCN-20",
                "COMPLETED",
                20L,
                baseSourceAt10.scenarios(),
                baseSourceAt10.emergencies(),
                baseSourceAt10.ambulances(),
                baseSourceAt10.hospitalReservations(),
                baseSourceAt10.operationResults(),
                baseSourceAt10.attemptLogs()
        );
        when(reportingService.generateReport("SCN-10", null))
                .thenReturn(new ReportingApplicationService.ReportViewResult(
                        ReportingCalculator.calculate(baseSourceAt10, null), baseSourceAt10.scenarios()));
        when(reportingService.generateReport("SCN-20", null))
                .thenReturn(new ReportingApplicationService.ReportViewResult(
                        ReportingCalculator.calculate(sameGlobalAt20, null), sameGlobalAt20.scenarios()));

        mockMvc.perform(get("/reporting").param("scenarioId", "SCN-10"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("60.00%")))
                .andExpect(content().string(containsString("data-met01=\"2.0\"")));

        mockMvc.perform(get("/reporting").param("scenarioId", "SCN-20"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("30.00%")))
                .andExpect(content().string(containsString("data-met01=\"2.0\"")));
    }

    @Test
    void rendersSupplementaryCardsAndHandlesInvalidTimeRangeGracefully() throws Exception {
        ReportingSourceData source = new ReportingSourceData(
                "SCN-10",
                "ACTIVE",
                12L,
                List.of(new ReportingSourceData.ScenarioRow("SCN-10", "Demo", "ACTIVE", 12L)),
                List.of(new ReportingSourceData.EmergencyRow(
                        "EMG-00001", "COMPLETED", "RUL-V01", "AMB-01", "HOSP-01",
                        "COMPLETED", 1L, 1L, 2L, 4L, null, 4L, 8L, "HIGH", "INC-A")),
                List.of(new ReportingSourceData.AmbulanceRow("AMB-01", "BASIC", "AVAILABLE", null)),
                List.of(),
                List.of(new ReportingSourceData.OperationResultRow(
                        "OP-1", "AUTOMATIC_DISPATCH", "EMG-00001", "AMB-01", 1, "ASSIGNED", "OK", 2L)),
                List.of(),
                List.of(new ReportingSourceData.HospitalRow("HOSP-01", "Hospital Central", "N03", true, 5, 1, 2))
        );
        SimulationReportSnapshot snapshot = ReportingCalculator.calculate(source, null);
        when(reportingService.generateReport("SCN-10", null))
                .thenReturn(new ReportingApplicationService.ReportViewResult(snapshot, source.scenarios()));

        String html = mockMvc.perform(get("/reporting")
                        .param("scenarioId", "SCN-10")
                        .param("fromTicks", "10")
                        .param("toTicks", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Distribución de emergencias")))
                .andExpect(content().string(containsString("Capacidad hospitalaria (C / R / O / D)")))
                .andExpect(content().string(containsString("Resultados de operaciones e intentos")))
                .andExpect(content().string(containsString("Hospital Central")))
                .andExpect(content().string(containsString("Exportar CSV")))
                .andExpect(content().string(containsString("El rango de tiempo lógico es inválido")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(HtmlStructureAssertions.countOccurrences(html, "<form")).isEqualTo(1);
    }

    @Test
    void exportsFilteredTraceabilityCsvAndRejectsInvalidCsvRange() throws Exception {
        ReportingSourceData source = new ReportingSourceData(
                "SCN-10",
                "ACTIVE",
                12L,
                List.of(new ReportingSourceData.ScenarioRow("SCN-10", "Demo", "ACTIVE", 12L)),
                List.of(new ReportingSourceData.EmergencyRow(
                        "EMG-00001", "COMPLETED", "RUL-V01", "AMB-01", "HOSP-01",
                        "COMPLETED", 3L, 3L, 4L, 6L, null, 6L, 9L, "HIGH", "INC-A")),
                List.of(new ReportingSourceData.AmbulanceRow("AMB-01", "BASIC", "AVAILABLE", null)),
                List.of(new ReportingSourceData.HospitalReservationRow("EMG-00001", "HOSP-01", "DISCHARGED", 11L)),
                List.of(),
                List.of(),
                List.of()
        );
        ReportingSupplementaryMetrics.TraceabilityFilter filter =
                new ReportingSupplementaryMetrics.TraceabilityFilter("COMPLETED", "HIGH", 1L, 10L);
        when(reportingService.generateFilteredReport("SCN-10", filter))
                .thenReturn(new ReportingApplicationService.ReportViewResult(
                        ReportingCalculator.calculateWithFilter(source, filter), source.scenarios()));

        byte[] body = mockMvc.perform(get("/reporting/export.csv")
                        .param("scenarioId", "SCN-10")
                        .param("state", "COMPLETED")
                        .param("priority", "HIGH")
                        .param("fromTicks", "1")
                        .param("toTicks", "10"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("are-reporte-trazabilidad.csv")))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        String csv = new String(body, StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\uFEFFemergency_id,state,priority,category,");
        assertThat(csv).contains("EMG-00001,COMPLETED,HIGH,INC-A,RUL-V01,AMB-01,HOSP-01,COMPLETED,3,3,4,6,,6,9,11,0,0");

        mockMvc.perform(get("/reporting/export.csv")
                        .param("fromTicks", "8")
                        .param("toTicks", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("El rango de tiempo lógico es inválido")));
    }
}

package com.arequiparescue.reporting.presentation;

import com.arequiparescue.reporting.application.ReportingApplicationService;
import com.arequiparescue.reporting.domain.ReportingCalculator;
import com.arequiparescue.reporting.domain.ReportingSourceData;
import com.arequiparescue.reporting.domain.SimulationReportSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportingController.class)
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

        mockMvc.perform(get("/reporting"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("MET-01")))
                .andExpect(content().string(containsString("MET-02")))
                .andExpect(content().string(containsString("MET-03")))
                .andExpect(content().string(containsString("MET-04")))
                .andExpect(content().string(containsString("MET-05")))
                .andExpect(content().string(containsString("MET-06")))
                .andExpect(content().string(containsString("MET-07")))
                .andExpect(content().string(containsString("Sin mediciones cerradas")))
                .andExpect(content().string(containsString("Registro funcional experimental trazable")));
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
}

package com.arequiparescue.reporting.presentation;

import com.arequiparescue.emergency.domain.EmergencyState;
import com.arequiparescue.emergency.domain.SimulatedPriority;
import com.arequiparescue.reporting.application.ReportingApplicationService;
import com.arequiparescue.reporting.domain.ReportingSupplementaryMetrics;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Controlador MVC de solo lectura para el panel de métricas experimentales, indicadores complementarios,
 * registro trazable y exportación CSV (HU-14, P1-2, ACT-03).
 */
@Controller
@RequestMapping("/reporting")
public class ReportingController {

    private static final MediaType TEXT_CSV_UTF8 = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final ReportingApplicationService reportingService;

    public ReportingController(ReportingApplicationService reportingService) {
        this.reportingService = Objects.requireNonNull(reportingService);
    }

    @GetMapping
    public String viewMetrics(
            @RequestParam(name = "scenarioId", required = false) String scenarioId,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "priority", required = false) String priority,
            @RequestParam(name = "fromTicks", required = false) Long fromTicks,
            @RequestParam(name = "toTicks", required = false) Long toTicks,
            Model model
    ) {
        String normalizedPriority = (priority == null || priority.isBlank()) ? "" : priority.trim();
        ReportingApplicationService.ReportViewResult result;
        String filterError = null;

        if (normalizedPriority.isEmpty() && fromTicks == null && toTicks == null) {
            result = reportingService.generateReport(scenarioId, state);
        } else {
            try {
                ReportingSupplementaryMetrics.TraceabilityFilter filter =
                        new ReportingSupplementaryMetrics.TraceabilityFilter(state, normalizedPriority, fromTicks, toTicks);
                result = reportingService.generateFilteredReport(scenarioId, filter);
            } catch (IllegalArgumentException ex) {
                filterError = ex.getMessage();
                result = reportingService.generateReport(scenarioId, state);
            }
        }

        model.addAttribute("report", result.snapshot());
        model.addAttribute("scenarios", result.scenarios());
        model.addAttribute("selectedScenarioId", result.snapshot().referenceScenarioId());
        model.addAttribute("selectedState", state == null ? "" : state.trim());
        model.addAttribute("selectedPriority", normalizedPriority);
        model.addAttribute("selectedFromTicks", fromTicks);
        model.addAttribute("selectedToTicks", toTicks);
        model.addAttribute("filterErrorMessage", filterError);
        model.addAttribute("states", EmergencyState.values());
        model.addAttribute("priorities", SimulatedPriority.values());
        return "reporting/metrics";
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportTraceabilityCsv(
            @RequestParam(name = "scenarioId", required = false) String scenarioId,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "priority", required = false) String priority,
            @RequestParam(name = "fromTicks", required = false) Long fromTicks,
            @RequestParam(name = "toTicks", required = false) Long toTicks
    ) {
        String normalizedPriority = (priority == null || priority.isBlank()) ? "" : priority.trim();
        ReportingApplicationService.ReportViewResult result;
        if (normalizedPriority.isEmpty() && fromTicks == null && toTicks == null) {
            result = reportingService.generateReport(scenarioId, state);
        } else {
            try {
                ReportingSupplementaryMetrics.TraceabilityFilter filter =
                        new ReportingSupplementaryMetrics.TraceabilityFilter(state, normalizedPriority, fromTicks, toTicks);
                result = reportingService.generateFilteredReport(scenarioId, filter);
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                        .body(ex.getMessage().getBytes(StandardCharsets.UTF_8));
            }
        }

        byte[] csvBytes = ReportingCsvExporter.exportTraceabilityCsv(result.snapshot().experimentalRecords());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(TEXT_CSV_UTF8);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("are-reporte-trazabilidad.csv", StandardCharsets.UTF_8)
                .build());
        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }
}

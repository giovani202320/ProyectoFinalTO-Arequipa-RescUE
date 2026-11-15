package com.arequiparescue.reporting.presentation;

import com.arequiparescue.emergency.domain.EmergencyState;
import com.arequiparescue.reporting.application.ReportingApplicationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Objects;

/**
 * Controlador MVC de solo lectura para el panel de métricas experimentales y registro trazable (HU-14, ACT-03).
 */
@Controller
@RequestMapping("/reporting")
public class ReportingController {

    private final ReportingApplicationService reportingService;

    public ReportingController(ReportingApplicationService reportingService) {
        this.reportingService = Objects.requireNonNull(reportingService);
    }

    @GetMapping
    public String viewMetrics(
            @RequestParam(name = "scenarioId", required = false) String scenarioId,
            @RequestParam(name = "state", required = false) String state,
            Model model
    ) {
        ReportingApplicationService.ReportViewResult result = reportingService.generateReport(scenarioId, state);
        model.addAttribute("report", result.snapshot());
        model.addAttribute("scenarios", result.scenarios());
        model.addAttribute("selectedScenarioId", result.snapshot().referenceScenarioId());
        model.addAttribute("selectedState", state == null ? "" : state.trim());
        model.addAttribute("states", EmergencyState.values());
        return "reporting/metrics";
    }
}

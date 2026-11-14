package com.arequiparescue.emergency.presentation;

import com.arequiparescue.emergency.application.EmergencyValidationException;
import com.arequiparescue.emergency.application.PublishTriageRuleVersionCommand;
import com.arequiparescue.emergency.application.TriageRuleCatalogApplicationService;
import com.arequiparescue.emergency.domain.TriageRuleVersion;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Controlador Spring MVC para la gestión de versiones inmutables del catálogo de reglas de triage
 * por el Administrador ({@code ACT-03}) y la consulta en modo de solo lectura para el Operador de
 * recepción ({@code ACT-01}) y el Despachador ({@code ACT-02}) conforme a {@code HU-04} y {@code DEC-009}.
 */
@Controller
public class TriageRuleCatalogController {

    private final TriageRuleCatalogApplicationService triageRuleCatalogService;

    public TriageRuleCatalogController(TriageRuleCatalogApplicationService triageRuleCatalogService) {
        this.triageRuleCatalogService = triageRuleCatalogService;
    }

    /**
     * Consulta en modo de solo lectura para Operador de recepción (ACT-01) y Despachador (ACT-02)
     * sin controles para crear, editar, publicar ni activar versiones (HU-04 CA-5, DEC-009).
     */
    @GetMapping("/triage-rules")
    public String showReadOnlyRules(
            @RequestParam(name = "actor", defaultValue = "ACT-01") String actor,
            Model model
    ) {
        String normalizedActor = "ACT-02".equalsIgnoreCase(actor) || "DESPACHADOR".equalsIgnoreCase(actor)
                ? "Despachador (ACT-02)"
                : "Operador de recepción (ACT-01)";
        model.addAttribute("viewerRoleLabel", normalizedActor);
        populateRuleVersions(model);
        return "emergencies/triage-rules-readonly";
    }

    /**
     * Panel administrativo para que el Administrador (ACT-03) consulte el historial inmutable
     * y publique/active nuevas versiones de reglas fuera de escenarios activos (HU-04 CA-1..CA-4).
     */
    @GetMapping("/admin/triage-rules")
    public String showAdminRulesPanel(Model model) {
        model.addAttribute("form", new PublishTriageRuleVersionForm());
        populateRuleVersions(model);
        return "emergencies/triage-rules-admin";
    }

    @PostMapping("/admin/triage-rules")
    public String publishAndActivateRules(
            @ModelAttribute("form") PublishTriageRuleVersionForm form,
            Model model,
            HttpServletResponse response
    ) {
        try {
            TriageRuleVersion published = triageRuleCatalogService.publishAndActivateNewVersion(
                    new PublishTriageRuleVersionCommand(form.toDraftItems())
            );
            model.addAttribute("publishedVersion", TriageRuleVersionView.fromDomain(published));
            model.addAttribute("form", new PublishTriageRuleVersionForm());
            populateRuleVersions(model);
            return "emergencies/triage-rules-admin";
        } catch (EmergencyValidationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateRuleVersions(model);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "emergencies/triage-rules-admin";
        }
    }

    private void populateRuleVersions(Model model) {
        TriageRuleVersionView activeVersion = triageRuleCatalogService.findActiveVersion()
                .map(TriageRuleVersionView::fromDomain)
                .orElse(null);
        List<TriageRuleVersionView> allVersions = triageRuleCatalogService.listAllVersions()
                .stream()
                .map(TriageRuleVersionView::fromDomain)
                .toList();
        model.addAttribute("activeVersion", activeVersion);
        model.addAttribute("versions", allVersions);
        model.addAttribute("scenarioActive", triageRuleCatalogService.isAnyScenarioActive());
    }
}

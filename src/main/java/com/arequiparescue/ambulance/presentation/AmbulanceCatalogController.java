package com.arequiparescue.ambulance.presentation;

import com.arequiparescue.ambulance.application.AmbulanceCatalogApplicationService;
import com.arequiparescue.ambulance.application.AmbulanceCatalogValidationException;
import com.arequiparescue.ambulance.application.RegisterAmbulanceCommand;
import com.arequiparescue.ambulance.application.UpdateAmbulanceCommand;
import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.routing.domain.GraphNode;
import com.arequiparescue.routing.domain.port.RoadGraphRepository;
import com.arequiparescue.simulation.domain.port.ActiveScenarioChecker;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Controlador Spring MVC para la administración del catálogo de ambulancias sobre el grafo
 * de 10 nodos por el Administrador ({@code ACT-03}, {@code HU-03}).
 */
@Controller
@RequestMapping("/admin/ambulances")
public class AmbulanceCatalogController {

    private final AmbulanceCatalogApplicationService ambulanceCatalogService;
    private final RoadGraphRepository roadGraphRepository;
    private final ActiveScenarioChecker activeScenarioChecker;

    public AmbulanceCatalogController(
            AmbulanceCatalogApplicationService ambulanceCatalogService,
            RoadGraphRepository roadGraphRepository,
            ActiveScenarioChecker activeScenarioChecker
    ) {
        this.ambulanceCatalogService = ambulanceCatalogService;
        this.roadGraphRepository = roadGraphRepository;
        this.activeScenarioChecker = activeScenarioChecker;
    }

    @GetMapping
    public String showAmbulanceCatalog(Model model) {
        model.addAttribute("form", new AmbulanceCatalogForm());
        populateModel(model);
        return "admin/ambulances";
    }

    @PostMapping
    public String registerAmbulance(
            @Valid @ModelAttribute("form") AmbulanceCatalogForm form,
            BindingResult bindingResult,
            Model model,
            HttpServletResponse response
    ) {
        if (bindingResult.hasErrors()) {
            populateModel(model);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "admin/ambulances";
        }
        try {
            Ambulance saved = ambulanceCatalogService.registerAmbulance(new RegisterAmbulanceCommand(
                    form.getId(),
                    form.getEquipmentType(),
                    form.getNodeId()
            ));
            model.addAttribute("savedAmbulance", AmbulanceView.fromDomain(saved));
            model.addAttribute("form", new AmbulanceCatalogForm());
            populateModel(model);
            return "admin/ambulances";
        } catch (AmbulanceCatalogValidationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateModel(model);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "admin/ambulances";
        }
    }

    @PostMapping("/{id}")
    public String updateAmbulance(
            @PathVariable("id") String id,
            @ModelAttribute("form") AmbulanceCatalogForm form,
            Model model,
            HttpServletResponse response
    ) {
        try {
            Ambulance updated = ambulanceCatalogService.updateAmbulance(new UpdateAmbulanceCommand(
                    id,
                    form.getEquipmentType(),
                    form.getNodeId()
            ));
            model.addAttribute("savedAmbulance", AmbulanceView.fromDomain(updated));
            model.addAttribute("form", new AmbulanceCatalogForm());
            populateModel(model);
            return "admin/ambulances";
        } catch (AmbulanceCatalogValidationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateModel(model);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "admin/ambulances";
        }
    }

    private void populateModel(Model model) {
        List<AmbulanceView> ambulances = ambulanceCatalogService.listAmbulances()
                .stream()
                .map(AmbulanceView::fromDomain)
                .toList();
        List<GraphNode> nodes = roadGraphRepository.loadGraph().nodes();
        model.addAttribute("ambulances", ambulances);
        model.addAttribute("nodes", nodes);
        model.addAttribute("scenarioActive", activeScenarioChecker.isAnyScenarioActive());
    }
}

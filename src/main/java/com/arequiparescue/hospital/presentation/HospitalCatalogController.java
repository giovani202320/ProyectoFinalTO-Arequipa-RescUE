package com.arequiparescue.hospital.presentation;

import com.arequiparescue.hospital.application.HospitalCatalogApplicationService;
import com.arequiparescue.hospital.application.HospitalCatalogValidationException;
import com.arequiparescue.hospital.application.RegisterHospitalCommand;
import com.arequiparescue.hospital.application.UpdateHospitalCommand;
import com.arequiparescue.hospital.domain.Hospital;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Objects;

/**
 * Controlador MVC para la administración y consulta del catálogo de hospitales sobre el grafo de 10 nodos
 * por el Administrador (ACT-03; HU-03, DEC-009, DEC-018, RN-HOS-001, RN-HOS-004, RN-HOS-015, CA-HOS-12).
 */
@Controller
@RequestMapping("/admin/hospitals")
public class HospitalCatalogController {

    private final HospitalCatalogApplicationService hospitalCatalogApplicationService;
    private final RoadGraphRepository roadGraphRepository;
    private final ActiveScenarioChecker activeScenarioChecker;

    public HospitalCatalogController(
            HospitalCatalogApplicationService hospitalCatalogApplicationService,
            RoadGraphRepository roadGraphRepository,
            ActiveScenarioChecker activeScenarioChecker
    ) {
        this.hospitalCatalogApplicationService = Objects.requireNonNull(hospitalCatalogApplicationService);
        this.roadGraphRepository = Objects.requireNonNull(roadGraphRepository);
        this.activeScenarioChecker = Objects.requireNonNull(activeScenarioChecker);
    }

    @GetMapping
    public String showHospitalCatalog(Model model) {
        if (!model.containsAttribute("hospitalForm")) {
            model.addAttribute("hospitalForm", new HospitalCatalogForm());
        }
        populateCatalogModel(model);
        return "admin/hospitals";
    }

    @PostMapping
    public String registerHospital(
            @Valid @ModelAttribute("hospitalForm") HospitalCatalogForm form,
            BindingResult bindingResult,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            populateCatalogModel(model);
            return "admin/hospitals";
        }

        try {
            Hospital created = hospitalCatalogApplicationService.registerHospital(
                    new RegisterHospitalCommand(
                            form.getId(),
                            form.getName(),
                            form.getNodeId(),
                            form.isEnabled(),
                            form.getTotalCapacity()
                    )
            );
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Hospital " + created.id().value() + " (" + created.name() + ") registrado correctamente en el nodo "
                            + created.locationNode().value() + " con capacidad total C = " + created.totalCapacity() + "."
            );
            return "redirect:/admin/hospitals";
        } catch (HospitalCatalogValidationException ex) {
            applyValidationException(bindingResult, ex);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            populateCatalogModel(model);
            return "admin/hospitals";
        }
    }

    @PostMapping("/{id}")
    public String updateHospital(
            @PathVariable("id") String id,
            @Valid @ModelAttribute("hospitalForm") HospitalCatalogForm form,
            BindingResult bindingResult,
            Model model,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes
    ) {
        form.setId(id);
        if (bindingResult.hasErrors()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            populateCatalogModel(model);
            return "admin/hospitals";
        }

        try {
            Hospital updated = hospitalCatalogApplicationService.updateHospital(
                    new UpdateHospitalCommand(
                            id,
                            form.getName(),
                            form.getNodeId(),
                            form.isEnabled(),
                            form.getTotalCapacity()
                    )
            );
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Hospital " + updated.id().value() + " (" + updated.name() + ") actualizado correctamente (C = "
                            + updated.totalCapacity() + ", R = " + updated.reservedSlots() + ", O = "
                            + updated.occupiedSlots() + ", D = " + updated.availableSlots() + ")."
            );
            return "redirect:/admin/hospitals";
        } catch (HospitalCatalogValidationException ex) {
            applyValidationException(bindingResult, ex);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            populateCatalogModel(model);
            return "admin/hospitals";
        }
    }

    private void applyValidationException(BindingResult bindingResult, HospitalCatalogValidationException ex) {
        if ("global".equals(ex.field())) {
            bindingResult.reject("hospitalCatalog.validation", ex.getMessage());
        } else {
            bindingResult.rejectValue(ex.field(), "hospitalCatalog.validation", ex.getMessage());
        }
    }

    private void populateCatalogModel(Model model) {
        List<HospitalView> hospitals = hospitalCatalogApplicationService.listHospitals()
                .stream()
                .map(HospitalView::fromDomain)
                .toList();
        model.addAttribute("hospitals", hospitals);
        model.addAttribute("nodes", roadGraphRepository.loadGraph().nodes());
        model.addAttribute("scenarioActive", activeScenarioChecker.isAnyScenarioActive());
    }
}

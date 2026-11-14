package com.arequiparescue.shared.presentation;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador MVC de la página de inicio del simulador ARE — Arequipa RescUE (Incremento 01).
 */
@Controller
public class HomeController {

    private static final HomeStatusView INCREMENT_01_STATUS = new HomeStatusView(
            "ARE — Arequipa RescUE",
            "Simulador académico de atención de emergencias médicas, despacho concurrente de ambulancias, "
                    + "cálculo de rutas mediante Dijkstra sobre una red vial simplificada de Arequipa y gestión "
                    + "simulada de capacidad hospitalaria.",
            "Estado de implementación: Incremento 01",
            "Monolito modular con principios de arquitectura hexagonal y presentación Spring MVC + Thymeleaf"
    );

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("status", INCREMENT_01_STATUS);
        return "home";
    }
}

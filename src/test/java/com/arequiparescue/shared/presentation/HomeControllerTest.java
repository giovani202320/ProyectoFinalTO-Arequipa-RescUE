package com.arequiparescue.shared.presentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HomeController.class)
@DisplayName("HomeController — Renderizado de GET / con Thymeleaf")
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET / responde HTTP 200 y renderiza la vista Thymeleaf con identidad de ARE y Centro de operaciones")
    void shouldRenderHomePage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(content().string(containsString("ARE — Arequipa RescUE")))
                .andExpect(content().string(containsString("Simulador académico de atención de emergencias médicas")))
                .andExpect(content().string(containsString("Centro de operaciones")))
                .andExpect(content().string(containsString("/vendor/bootstrap/5.3.3/css/bootstrap.min.css")))
                .andExpect(content().string(containsString("/css/are-design-system.css")))
                .andExpect(content().string(containsString("/vendor/lucide/0.469.0/lucide.min.js")))
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andExpect(content().string(containsString("Módulos operativos")));
    }
}

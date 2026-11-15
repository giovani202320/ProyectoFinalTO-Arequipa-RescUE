package com.arequiparescue.shared.presentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AreUiFormatter — Traducción contextual al español y tonos semánticos")
class AreUiFormatterTest {

    private final AreUiFormatter formatter = new AreUiFormatter();

    @Test
    @DisplayName("Traduce todos los estados de emergencia y distingue códigos compartidos por contexto")
    void shouldTranslateEmergencyAndAmbulanceStatesByContext() {
        assertThat(formatter.emergencyStateLabel("REGISTERED")).isEqualTo("Registrada");
        assertThat(formatter.emergencyStateLabel("WAITING_AMBULANCE")).isEqualTo("Esperando ambulancia");
        assertThat(formatter.emergencyStateLabel("ASSIGNED")).isEqualTo("Ambulancia asignada");
        assertThat(formatter.emergencyStateLabel("AT_INCIDENT")).isEqualTo("En el incidente");
        assertThat(formatter.emergencyStateLabel("WAITING_HOSPITAL")).isEqualTo("Esperando hospital");
        assertThat(formatter.emergencyStateLabel("IN_TRANSPORT")).isEqualTo("En traslado");
        assertThat(formatter.emergencyStateLabel("COMPLETED")).isEqualTo("Atención finalizada");

        assertThat(formatter.ambulanceStateLabel("AVAILABLE")).isEqualTo("Disponible");
        assertThat(formatter.ambulanceStateLabel("ASSIGNED")).isEqualTo("Asignada a emergencia");
        assertThat(formatter.ambulanceStateLabel("AT_INCIDENT")).isEqualTo("En el incidente");
        assertThat(formatter.ambulanceStateLabel("TRANSPORTING")).isEqualTo("En traslado");
    }

    @Test
    @DisplayName("Traduce equipamiento, escenarios, eventos, reglas y prioridades con fallback seguro")
    void shouldTranslateRemainingCatalogsSafely() {
        assertThat(formatter.equipmentLabel("BASIC")).isEqualTo("Básica");
        assertThat(formatter.equipmentLabel("SPECIALIZED")).isEqualTo("Especializada");

        assertThat(formatter.scenarioStatusLabel("ACTIVE")).isEqualTo("Activo");
        assertThat(formatter.scenarioStatusLabel("COMPLETED")).isEqualTo("Finalizado");

        assertThat(formatter.eventStatusLabel("PENDING")).isEqualTo("Pendiente");
        assertThat(formatter.eventStatusLabel("EXECUTED")).isEqualTo("Ejecutado");
        assertThat(formatter.eventStatusLabel("FAILED")).isEqualTo("Fallido");

        assertThat(formatter.eventTypeLabel("AMBULANCE_ARRIVAL_AT_INCIDENT"))
                .isEqualTo("Llegada de ambulancia al incidente");
        assertThat(formatter.ruleVersionStatusLabel("ACTIVE")).isEqualTo("Activa");
        assertThat(formatter.priorityLabel("HIGH")).isEqualTo("Alta");

        assertThat(formatter.emergencyStateLabel(null)).isEqualTo("No especificado");
        assertThat(formatter.emergencyStateLabel("UNKNOWN_CODE")).isEqualTo("UNKNOWN_CODE");
    }

    @Test
    @DisplayName("Asigna tonos semánticos consistentes por contexto de dominio")
    void shouldResolveBadgeTonesByContext() {
        assertThat(formatter.badgeTone("EMERGENCY", "COMPLETED")).isEqualTo("success");
        assertThat(formatter.badgeTone("EMERGENCY", "WAITING_AMBULANCE")).isEqualTo("warning");
        assertThat(formatter.badgeTone("AMBULANCE", "AVAILABLE")).isEqualTo("success");
        assertThat(formatter.badgeTone("EQUIPMENT", "SPECIALIZED")).isEqualTo("navy");
        assertThat(formatter.badgeTone("EVENT", "FAILED")).isEqualTo("danger");
    }
}

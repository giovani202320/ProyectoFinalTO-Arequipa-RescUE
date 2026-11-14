package com.arequiparescue.emergency.presentation;

/**
 * DTO de presentación para poblar las opciones de categoría de incidente en el formulario Thymeleaf (HU-01).
 */
public record CategoryOptionView(
        String code,
        String label
) {
}

package com.arequiparescue.emergency.presentation;

/**
 * DTO de presentación para poblar las opciones de nodo del grafo vial en el formulario Thymeleaf (HU-01).
 */
public record NodeOptionView(
        String id,
        String displayName,
        boolean isolated
) {
}

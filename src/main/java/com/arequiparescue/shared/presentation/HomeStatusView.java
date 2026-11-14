package com.arequiparescue.shared.presentation;

/**
 * DTO de presentación para la vista inicial del Incremento 01 (DEC-002, DEC-003).
 */
public record HomeStatusView(
        String systemTitle,
        String academicDescription,
        String implementationStage,
        String architectureSummary
) {
}

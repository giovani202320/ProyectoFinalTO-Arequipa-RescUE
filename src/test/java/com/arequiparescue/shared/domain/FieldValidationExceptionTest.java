package com.arequiparescue.shared.domain;

import com.arequiparescue.ambulance.application.AmbulanceCatalogValidationException;
import com.arequiparescue.emergency.application.EmergencyValidationException;
import com.arequiparescue.hospital.application.HospitalCatalogValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FieldValidationException — Herencia y polimorfismo de excepciones de validación (B1)")
class FieldValidationExceptionTest {

    @Test
    @DisplayName("Centraliza field() y mensaje en clase abstracta unchecked preservando subtipos concretos")
    void shouldInheritFieldAndMessageAcrossSpecializedExceptions() {
        assertThat(Modifier.isAbstract(FieldValidationException.class.getModifiers())).isTrue();

        EmergencyValidationException emgEx =
                new EmergencyValidationException("locationNode", "Nodo inválido.");
        AmbulanceCatalogValidationException ambEx =
                new AmbulanceCatalogValidationException("equipmentType", "Equipamiento inválido.");
        HospitalCatalogValidationException hospEx =
                new HospitalCatalogValidationException("totalCapacity", "Capacidad negativa.");
        HospitalCatalogValidationException hospGlobalEx =
                new HospitalCatalogValidationException("Error global hospitalario.");

        assertThat(emgEx).isInstanceOf(FieldValidationException.class).isInstanceOf(RuntimeException.class);
        assertThat(ambEx).isInstanceOf(FieldValidationException.class).isInstanceOf(RuntimeException.class);
        assertThat(hospEx).isInstanceOf(FieldValidationException.class).isInstanceOf(RuntimeException.class);

        assertThat(emgEx.field()).isEqualTo("locationNode");
        assertThat(emgEx.getMessage()).isEqualTo("Nodo inválido.");
        assertThat(ambEx.field()).isEqualTo("equipmentType");
        assertThat(ambEx.getMessage()).isEqualTo("Equipamiento inválido.");
        assertThat(hospEx.field()).isEqualTo("totalCapacity");
        assertThat(hospEx.getMessage()).isEqualTo("Capacidad negativa.");
        assertThat(hospGlobalEx.field()).isNull();
        assertThat(hospGlobalEx.getMessage()).isEqualTo("Error global hospitalario.");
    }

    @Test
    @DisplayName("Permite tratamiento polimórfico uniforme mediante la referencia base FieldValidationException")
    void shouldSupportPolymorphicHandlingThroughBaseType() {
        List<FieldValidationException> validations = List.of(
                new EmergencyValidationException("incidentCategory", "Categoría obligatoria."),
                new AmbulanceCatalogValidationException("id", "ID duplicado."),
                new HospitalCatalogValidationException("name", "Nombre obligatorio.")
        );

        List<String> formatted = validations.stream()
                .map(ex -> ex.field() + ":" + ex.getMessage())
                .toList();

        assertThat(formatted).containsExactly(
                "incidentCategory:Categoría obligatoria.",
                "id:ID duplicado.",
                "name:Nombre obligatorio."
        );
    }
}

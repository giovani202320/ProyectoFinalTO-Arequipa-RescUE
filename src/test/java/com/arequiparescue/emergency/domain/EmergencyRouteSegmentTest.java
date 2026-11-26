package com.arequiparescue.emergency.domain;

import com.arequiparescue.routing.domain.NodeId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmergencyRouteSegmentTest {

    @Test
    @DisplayName("Construye segmento válido inmutable cuando cumple todas las invariantes de ADR-0003")
    void shouldCreateValidImmutableSegment() {
        List<NodeId> mutablePath = new ArrayList<>(List.of(NodeId.of("N01"), NodeId.of("N03"), NodeId.of("N05")));

        EmergencyRouteSegment segment = new EmergencyRouteSegment(
                EmergencyId.of("EMG-00001"),
                RouteSegmentType.AMBULANCE_TO_INCIDENT,
                " AMB-01 ",
                NodeId.of("N01"),
                NodeId.of("N05"),
                mutablePath,
                9,
                12L
        );

        mutablePath.add(NodeId.of("N07"));

        assertThat(segment.resourceId()).isEqualTo("AMB-01");
        assertThat(segment.pathNodes()).containsExactly(NodeId.of("N01"), NodeId.of("N03"), NodeId.of("N05"));
        assertThat(segment.abstractCost()).isEqualTo(9);
        assertThat(segment.calculatedAtTicks()).isEqualTo(12L);
    }

    @Test
    @DisplayName("Permite segmento de un solo nodo cuando origen y destino coinciden con costo cero")
    void shouldAllowSingleNodeSegmentWhenOriginEqualsDestination() {
        EmergencyRouteSegment segment = new EmergencyRouteSegment(
                EmergencyId.of("EMG-00002"),
                RouteSegmentType.INCIDENT_TO_HOSPITAL,
                "HOSP-01",
                NodeId.of("N02"),
                NodeId.of("N02"),
                List.of(NodeId.of("N02")),
                0,
                0L
        );

        assertThat(segment.pathNodes()).containsExactly(NodeId.of("N02"));
        assertThat(segment.abstractCost()).isZero();
    }

    @Test
    @DisplayName("Rechaza campos nulos, recurso vacío, secuencia vacía, extremos inconsistentes o valores negativos")
    void shouldRejectInvalidInvariants() {
        assertThatThrownBy(() -> new EmergencyRouteSegment(
                null, RouteSegmentType.AMBULANCE_TO_INCIDENT, "AMB-01",
                NodeId.of("N01"), NodeId.of("N02"), List.of(NodeId.of("N01"), NodeId.of("N02")), 4, 1L))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new EmergencyRouteSegment(
                EmergencyId.of("EMG-00001"), null, "AMB-01",
                NodeId.of("N01"), NodeId.of("N02"), List.of(NodeId.of("N01"), NodeId.of("N02")), 4, 1L))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new EmergencyRouteSegment(
                EmergencyId.of("EMG-00001"), RouteSegmentType.AMBULANCE_TO_INCIDENT, "   ",
                NodeId.of("N01"), NodeId.of("N02"), List.of(NodeId.of("N01"), NodeId.of("N02")), 4, 1L))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new EmergencyRouteSegment(
                EmergencyId.of("EMG-00001"), RouteSegmentType.AMBULANCE_TO_INCIDENT, "AMB-01",
                NodeId.of("N01"), NodeId.of("N02"), List.of(), 4, 1L))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new EmergencyRouteSegment(
                EmergencyId.of("EMG-00001"), RouteSegmentType.AMBULANCE_TO_INCIDENT, "AMB-01",
                NodeId.of("N01"), NodeId.of("N02"), List.of(NodeId.of("N03"), NodeId.of("N02")), 4, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("primer nodo");

        assertThatThrownBy(() -> new EmergencyRouteSegment(
                EmergencyId.of("EMG-00001"), RouteSegmentType.INCIDENT_TO_HOSPITAL, "HOSP-01",
                NodeId.of("N01"), NodeId.of("N02"), List.of(NodeId.of("N01"), NodeId.of("N04")), 4, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("último nodo");

        assertThatThrownBy(() -> new EmergencyRouteSegment(
                EmergencyId.of("EMG-00001"), RouteSegmentType.INCIDENT_TO_HOSPITAL, "HOSP-01",
                NodeId.of("N01"), NodeId.of("N02"), List.of(NodeId.of("N01"), NodeId.of("N02")), -1, 1L))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new EmergencyRouteSegment(
                EmergencyId.of("EMG-00001"), RouteSegmentType.INCIDENT_TO_HOSPITAL, "HOSP-01",
                NodeId.of("N01"), NodeId.of("N02"), List.of(NodeId.of("N01"), NodeId.of("N02")), 4, -1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

package com.arequiparescue.simulation.domain;

import com.arequiparescue.shared.domain.LogicalClock;
import com.arequiparescue.shared.domain.LogicalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DiscreteEventScheduler — Ordenamiento determinista y avance manual (DEC-019, RN-RUT-009, CA-SIM-06, CA-SIM-07)")
class DiscreteEventSchedulerTest {

    @Test
    @DisplayName("Procesa eventos en orden creciente de tiempo lógico y avanza el reloj monotónicamente")
    void shouldProcessEventsInChronologicalLogicalOrderAndAdvanceClock() {
        DiscreteEventScheduler scheduler = new DiscreteEventScheduler();

        scheduler.schedule("EVT-003", LogicalTime.of(8L), DiscreteEventType.RESOURCE_RELEASE, "AMB-01", "Entrega hospitalaria");
        scheduler.schedule("EVT-001", LogicalTime.of(2L), DiscreteEventType.EMERGENCY_REGISTRATION, "EMG-00001", "Registro inicial");
        scheduler.schedule("EVT-002", LogicalTime.of(5L), DiscreteEventType.AMBULANCE_ARRIVAL_AT_INCIDENT, "EMG-00001", "Llegada al incidente");

        assertThat(scheduler.currentTime()).isEqualTo(LogicalTime.ZERO);

        DiscreteEvent first = scheduler.stepNext().orElseThrow();
        assertThat(first.eventId()).isEqualTo("EVT-001");
        assertThat(scheduler.currentTime()).isEqualTo(LogicalTime.of(2L));

        DiscreteEvent second = scheduler.stepNext().orElseThrow();
        assertThat(second.eventId()).isEqualTo("EVT-002");
        assertThat(scheduler.currentTime()).isEqualTo(LogicalTime.of(5L));

        DiscreteEvent third = scheduler.stepNext().orElseThrow();
        assertThat(third.eventId()).isEqualTo("EVT-003");
        assertThat(scheduler.currentTime()).isEqualTo(LogicalTime.of(8L));

        assertThat(scheduler.stepNext()).isEmpty();
    }

    @Test
    @DisplayName("Desempata eventos simultáneos (t_a = t_b) por precedencia fija de tipo de evento (CA-SIM-07)")
    void shouldBreakTiesByEventTypePrecedenceAtSameLogicalTime() {
        DiscreteEventScheduler scheduler = new DiscreteEventScheduler();
        LogicalTime simultaneousTime = LogicalTime.of(10L);

        // Registrados en orden inverso a su precedencia para comprobar el ordenamiento determinista
        scheduler.schedule("EVT-REG", simultaneousTime, DiscreteEventType.EMERGENCY_REGISTRATION, "EMG-00004", "4. Registro emergencia");
        scheduler.schedule("EVT-QUE", simultaneousTime, DiscreteEventType.WAITING_QUEUE_EVALUATION, "EMG-00003", "3. Evaluación cola");
        scheduler.schedule("EVT-ARR", simultaneousTime, DiscreteEventType.AMBULANCE_ARRIVAL_AT_INCIDENT, "EMG-00002", "2. Llegada incidente");
        scheduler.schedule("EVT-REL", simultaneousTime, DiscreteEventType.RESOURCE_RELEASE, "EMG-00001", "1. Liberación recurso");

        List<String> executedOrder = new ArrayList<>();
        while (scheduler.hasPendingEvents()) {
            executedOrder.add(scheduler.stepNext().orElseThrow().eventId());
        }

        assertThat(executedOrder).containsExactly("EVT-REL", "EVT-ARR", "EVT-QUE", "EVT-REG");
        assertThat(scheduler.currentTime()).isEqualTo(simultaneousTime);
    }

    @Test
    @DisplayName("Desempata eventos del mismo instante y mismo tipo por secuencia monotónica creciente y luego por ID estable")
    void shouldBreakTiesWithinSameTypeBySequenceAndThenByEventId() {
        DiscreteEventScheduler scheduler = new DiscreteEventScheduler();
        LogicalTime instant = LogicalTime.of(4L);

        scheduler.register(new DiscreteEvent(
                "EVT-B", instant, DiscreteEventType.WAITING_QUEUE_EVALUATION, 20L, "EMG-00002", "Secuencia 20"));
        scheduler.register(new DiscreteEvent(
                "EVT-Z", instant, DiscreteEventType.WAITING_QUEUE_EVALUATION, 10L, "EMG-00001", "Secuencia 10"));
        scheduler.register(new DiscreteEvent(
                "EVT-A", instant, DiscreteEventType.WAITING_QUEUE_EVALUATION, 20L, "EMG-00003", "Secuencia 20 con menor ID"));

        List<String> executedOrder = new ArrayList<>();
        while (scheduler.hasPendingEvents()) {
            executedOrder.add(scheduler.stepNext().orElseThrow().eventId());
        }

        assertThat(executedOrder).containsExactly("EVT-Z", "EVT-A", "EVT-B");
    }

    @Test
    @DisplayName("Rechaza registrar eventos en un instante lógico anterior al tiempo actual del reloj")
    void shouldRejectSchedulingEventInLogicalPast() {
        LogicalClock clock = new LogicalClock(LogicalTime.of(6L));
        DiscreteEventScheduler scheduler = new DiscreteEventScheduler(clock);

        assertThatThrownBy(() -> scheduler.schedule(
                "EVT-PAST",
                LogicalTime.of(5L),
                DiscreteEventType.EMERGENCY_REGISTRATION,
                "EMG-00001",
                "Evento inválido en el pasado"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pasado lógico");
    }
}

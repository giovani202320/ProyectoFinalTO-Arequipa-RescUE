package com.arequiparescue.simulation.domain;

import com.arequiparescue.shared.domain.LogicalTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationScenarioTest {

    @Test
    void startsActiveAtZeroAndAdvancesMonotonically() {
        SimulationScenario scenario = SimulationScenario.startNew("SCN-01", "Escenario Base");

        assertTrue(scenario.isActive());
        assertEquals(ScenarioStatus.ACTIVE, scenario.status());
        assertEquals(LogicalTime.ZERO, scenario.currentTime());

        scenario.validateCanScheduleAt(LogicalTime.of(5));
        assertEquals(LogicalTime.of(5), scenario.advanceClockTo(LogicalTime.of(5)));
        assertEquals(LogicalTime.of(5), scenario.advanceClockTo(LogicalTime.of(5)));
        assertEquals(LogicalTime.of(12), scenario.advanceClockTo(LogicalTime.of(12)));
    }

    @Test
    void rejectsClockRegressionAndPastEvents() {
        SimulationScenario scenario = SimulationScenario.startNew("SCN-02", "Monotonicidad");
        scenario.advanceClockTo(LogicalTime.of(10));

        assertThrows(IllegalArgumentException.class, () -> scenario.advanceClockTo(LogicalTime.of(9)));
        assertThrows(IllegalArgumentException.class, () -> scenario.validateCanScheduleAt(LogicalTime.of(9)));
    }

    @Test
    void completedScenarioRejectsAdvanceScheduleAndSecondComplete() {
        SimulationScenario scenario = SimulationScenario.startNew("SCN-03", "Cierre");
        scenario.complete();

        assertFalse(scenario.isActive());
        assertEquals(ScenarioStatus.COMPLETED, scenario.status());
        assertThrows(IllegalStateException.class, () -> scenario.advanceClockTo(LogicalTime.of(1)));
        assertThrows(IllegalStateException.class, () -> scenario.validateCanScheduleAt(LogicalTime.of(1)));
        assertThrows(IllegalStateException.class, scenario::complete);
    }

    @Test
    void scheduledEventTransitionsToExecutedOrFailed() {
        DiscreteEvent raw = new DiscreteEvent(
                "EV-01",
                LogicalTime.of(4),
                DiscreteEventType.WAITING_QUEUE_EVALUATION,
                1L,
                "SYS",
                "Evaluación"
        );
        ScheduledSimulationEvent event = ScheduledSimulationEvent.pending("SCN-01", raw);
        assertTrue(event.isPending());

        event.markExecuted(LogicalTime.of(4), "Ejecutado correctamente");
        assertEquals(DiscreteEventStatus.EXECUTED, event.status());
        assertEquals(LogicalTime.of(4), event.executedAt().orElseThrow());
        assertThrows(IllegalStateException.class, () -> event.markExecuted(LogicalTime.of(5), "Duplicado"));

        ScheduledSimulationEvent failed = ScheduledSimulationEvent.pending("SCN-01", raw);
        failed.markFailed(LogicalTime.of(4), "Fallo controlado");
        assertEquals(DiscreteEventStatus.FAILED, failed.status());
    }
}

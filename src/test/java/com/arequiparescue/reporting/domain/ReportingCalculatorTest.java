package com.arequiparescue.reporting.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReportingCalculatorTest {

    @Test
    void handlesEmptyDataAndZeroElapsedLogicalTimeWithoutDivisionByZero() {
        ReportingSourceData empty = new ReportingSourceData(
                null,
                "SIN_ESCENARIO",
                0L,
                List.of(),
                List.of(),
                List.of(new ReportingSourceData.AmbulanceRow("AMB-01", "BASIC", "AVAILABLE", null)),
                List.of(),
                List.of(),
                List.of()
        );

        SimulationReportSnapshot report = ReportingCalculator.calculate(empty, null);

        assertEquals(0L, report.currentLogicalTicks());
        assertNull(report.waitingForAmbulance().closedAverageTicks());
        assertNull(report.responseToIncident().closedAverageTicks());
        assertNull(report.pickupToHospital().closedAverageTicks());
        assertNull(report.waitingForHospital().closedAverageTicks());
        assertEquals(0L, report.completedEmergencies().completedCount());
        assertEquals(0L, report.completedEmergencies().activeCount());
        assertEquals(0L, report.ambulanceUtilization().totalOccupiedTicks());
        assertEquals(0.0, report.ambulanceUtilization().averageUtilizationRatio());
        assertEquals(0L, report.concurrencyConflicts().totalConflicts());
        assertEquals(0, report.experimentalRecords().size());
    }

    @Test
    void computesAllSevenMetricsSeparatingClosedFromInProgressAndStarvation() {
        List<ReportingSourceData.EmergencyRow> emergencies = List.of(
                // EMG-00001: ciclo completo, reserva inmediata en hospital (espera hospitalaria = 0)
                new ReportingSourceData.EmergencyRow(
                        "EMG-00001", "COMPLETED", "RUL-V01", "AMB-01", "HOSP-01", "COMPLETED",
                        2L, 2L, 5L, 8L, null, 8L, 12L
                ),
                // EMG-00002: en cola de ambulancia desde t=4 (t_actual=20 -> acumulado 16)
                new ReportingSourceData.EmergencyRow(
                        "EMG-00002", "WAITING_AMBULANCE", "RUL-V01", null, null, "NO_ELIGIBLE_AMBULANCE",
                        4L, 4L, null, null, null, null, null
                ),
                // EMG-00003: en espera hospitalaria desde t=15 con ambulancia AMB-02 asignada desde t=10
                new ReportingSourceData.EmergencyRow(
                        "EMG-00003", "WAITING_HOSPITAL", "RUL-V01", "AMB-02", null, "HOSPITAL_RESERVATION_RETRY_EXHAUSTED",
                        9L, 9L, 10L, 14L, 15L, null, null
                )
        );

        List<ReportingSourceData.AmbulanceRow> ambulances = List.of(
                new ReportingSourceData.AmbulanceRow("AMB-01", "BASIC", "AVAILABLE", null),
                new ReportingSourceData.AmbulanceRow("AMB-02", "SPECIALIZED", "AT_INCIDENT", "EMG-00003")
        );

        List<ReportingSourceData.HospitalReservationRow> reservations = List.of(
                new ReportingSourceData.HospitalReservationRow("EMG-00001", "HOSP-01", "DISCHARGED", 16L)
        );

        List<ReportingSourceData.OperationResultRow> results = List.of(
                new ReportingSourceData.OperationResultRow(
                        "OP-1", "AUTOMATIC_DISPATCH", "EMG-00001", "AMB-01", 2, "ASSIGNED", "OK", 5L),
                new ReportingSourceData.OperationResultRow(
                        "OP-2", "AUTOMATIC_DISPATCH", "EMG-00002", null, 4, "CONCURRENCY_RETRY_EXHAUSTED", "Agotado", 4L),
                new ReportingSourceData.OperationResultRow(
                        "OP-3", "HOSPITAL_RESERVATION", "EMG-00003", null, 3, "HOSPITAL_RESERVATION_RETRY_EXHAUSTED", "Agotado hosp", 15L)
        );

        List<ReportingSourceData.AttemptLogRow> logs = List.of(
                new ReportingSourceData.AttemptLogRow("OP-1", "AUTOMATIC_DISPATCH", "EMG-00001", "AMB-01", 1, "AMBULANCE_UNAVAILABLE_CONFLICT", "Conflicto", 5L),
                new ReportingSourceData.AttemptLogRow("OP-1", "AUTOMATIC_DISPATCH", "EMG-00001", "AMB-01", 2, "ASSIGNED", "OK", 5L),
                new ReportingSourceData.AttemptLogRow("OP-2", "AUTOMATIC_DISPATCH", "EMG-00002", "AMB-01", 1, "AMBULANCE_UNAVAILABLE_CONFLICT", "C1", 4L),
                new ReportingSourceData.AttemptLogRow("OP-2", "AUTOMATIC_DISPATCH", "EMG-00002", "AMB-01", 2, "AMBULANCE_UNAVAILABLE_CONFLICT", "C2", 4L),
                new ReportingSourceData.AttemptLogRow("OP-3", "HOSPITAL_RESERVATION", "EMG-00003", "HOSP-01", 1, "HOSPITAL_CAPACITY_CONFLICT", "H1", 15L),
                new ReportingSourceData.AttemptLogRow("OP-3", "HOSPITAL_RESERVATION", "EMG-00003", "HOSP-01", 2, "HOSPITAL_CAPACITY_CONFLICT", "H2", 15L),
                new ReportingSourceData.AttemptLogRow("OP-3", "HOSPITAL_RESERVATION", "EMG-00003", "HOSP-01", 3, "HOSPITAL_CAPACITY_CONFLICT", "H3", 15L)
        );

        ReportingSourceData source = new ReportingSourceData(
                "SCN-01", "ACTIVE", 20L,
                List.of(new ReportingSourceData.ScenarioRow("SCN-01", "Exp", "ACTIVE", 20L)),
                emergencies, ambulances, reservations, results, logs
        );

        SimulationReportSnapshot report = ReportingCalculator.calculate(source, null);

        // Métrica 1: cerrados = EMG-00001 (5-2=3), EMG-00003 (10-9=1) -> promedio = 2.0; en curso = EMG-00002 (20-4=16)
        assertEquals(2L, report.waitingForAmbulance().closedCount());
        assertEquals(2.0, report.waitingForAmbulance().closedAverageTicks());
        assertEquals(1L, report.waitingForAmbulance().inProgressCount());
        assertEquals("EN_ESPERA", report.waitingForAmbulance().inProgressStatusLabel());
        assertEquals(16L, report.waitingForAmbulance().inProgressItems().getFirst().accumulatedTicks());

        // Métrica 2: cerrados = EMG-00001 (8-2=6), EMG-00003 (14-9=5) -> promedio = 5.5; en curso = EMG-00002 (20-4=16)
        assertEquals(2L, report.responseToIncident().closedCount());
        assertEquals(5.5, report.responseToIncident().closedAverageTicks());
        assertEquals(1L, report.responseToIncident().inProgressCount());

        // Métrica 3: cerrado = EMG-00001 (12-8=4); en curso = EMG-00003 (20-14=6)
        assertEquals(1L, report.pickupToHospital().closedCount());
        assertEquals(4.0, report.pickupToHospital().closedAverageTicks());
        assertEquals(1L, report.pickupToHospital().inProgressCount());

        // Métrica 4: 1 completada, 2 activas
        assertEquals(1L, report.completedEmergencies().completedCount());
        assertEquals(2L, report.completedEmergencies().activeCount());

        // Métrica 5: AMB-01 = 12-5 = 7 u.; AMB-02 = 20-10 = 10 u.; total = 17 u. sobre 2*20 = 40
        assertEquals(17L, report.ambulanceUtilization().totalOccupiedTicks());
        assertEquals(7L, report.ambulanceUtilization().items().get(0).occupiedTicks());
        assertEquals(10L, report.ambulanceUtilization().items().get(1).occupiedTicks());
        assertEquals(17.0 / 40.0, report.ambulanceUtilization().averageUtilizationRatio(), 0.0001);

        // Métrica 6: despacho (3 conflictos, 2 reintentos, 1 agotamiento), hospital (3 conflictos, 2 reintentos, 1 agotamiento)
        assertEquals(3L, report.concurrencyConflicts().ambulanceDispatch().conflicts());
        assertEquals(2L, report.concurrencyConflicts().ambulanceDispatch().retriesConsumed());
        assertEquals(1L, report.concurrencyConflicts().ambulanceDispatch().exhaustions());
        assertEquals(3L, report.concurrencyConflicts().hospitalReservation().conflicts());
        assertEquals(2L, report.concurrencyConflicts().hospitalReservation().retriesConsumed());
        assertEquals(1L, report.concurrencyConflicts().hospitalReservation().exhaustions());

        // Métrica 7: EMG-00001 = 0 (primer ciclo), EMG-00003 en curso = 20-15 = 5
        assertEquals(1L, report.waitingForHospital().closedCount());
        assertEquals(0.0, report.waitingForHospital().closedAverageTicks());
        assertEquals(1L, report.waitingForHospital().inProgressCount());
        assertEquals(5L, report.waitingForHospital().inProgressItems().getFirst().accumulatedTicks());

        // Filtro por estado en registro trazable
        SimulationReportSnapshot filtered = ReportingCalculator.calculate(source, "COMPLETED");
        assertEquals(1, filtered.experimentalRecords().size());
        assertEquals(16L, filtered.experimentalRecords().getFirst().dischargedAtTicks());
    }
}

package com.arequiparescue.reporting.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void computesMultiAmbulanceUtilizationAccuratelyWithoutMaskingMultiScenarioOverflow() {
        // Caso 6 y 7: 3 ambulancias controladas con Δt = 10:
        // - AMB-01: dos emergencias completadas acumuladas (7 + 5 = 12 u. > Δt=10 -> 1.20 sin truncamiento artificial)
        // - AMB-02: activa en curso desde t=4 hasta t=10 -> 6 u. -> 0.60
        // - AMB-03: ociosa -> 0 u. -> 0.00
        // Promedio flota: (12 + 6 + 0) / (3 * 10) = 18 / 30 = 0.60
        List<ReportingSourceData.EmergencyRow> emergencies = List.of(
                new ReportingSourceData.EmergencyRow(
                        "EMG-00001", "COMPLETED", "RUL-V01", "AMB-01", "HOSP-01", "COMPLETED",
                        0L, 0L, 0L, 3L, null, 3L, 7L),
                new ReportingSourceData.EmergencyRow(
                        "EMG-00002", "COMPLETED", "RUL-V01", "AMB-01", "HOSP-01", "COMPLETED",
                        0L, 0L, 0L, 2L, null, 2L, 5L),
                new ReportingSourceData.EmergencyRow(
                        "EMG-00003", "IN_TRANSPORT", "RUL-V01", "AMB-02", "HOSP-01", null,
                        3L, 3L, 4L, 6L, null, 6L, null)
        );
        List<ReportingSourceData.AmbulanceRow> ambulances = List.of(
                new ReportingSourceData.AmbulanceRow("AMB-01", "BASIC", "AVAILABLE", null),
                new ReportingSourceData.AmbulanceRow("AMB-02", "SPECIALIZED", "TRANSPORTING", "EMG-00003"),
                new ReportingSourceData.AmbulanceRow("AMB-03", "BASIC", "AVAILABLE", null)
        );

        ReportingSourceData source = new ReportingSourceData(
                "SCN-02", "COMPLETED", 10L,
                List.of(new ReportingSourceData.ScenarioRow("SCN-02", "Multi", "COMPLETED", 10L)),
                emergencies, ambulances, List.of(), List.of(), List.of()
        );

        SimulationReportSnapshot report = ReportingCalculator.calculate(source, null);

        assertEquals(10L, report.ambulanceUtilization().scenarioElapsedTicks());
        assertEquals(18L, report.ambulanceUtilization().totalOccupiedTicks());
        assertEquals(12L, report.ambulanceUtilization().items().get(0).occupiedTicks());
        assertEquals(1.2, report.ambulanceUtilization().items().get(0).utilizationRatio(), 0.0001);
        assertEquals(6L, report.ambulanceUtilization().items().get(1).occupiedTicks());
        assertEquals(0.6, report.ambulanceUtilization().items().get(1).utilizationRatio(), 0.0001);
        assertEquals(0L, report.ambulanceUtilization().items().get(2).occupiedTicks());
        assertEquals(0.0, report.ambulanceUtilization().items().get(2).utilizationRatio(), 0.0001);
        assertEquals(0.6, report.ambulanceUtilization().averageUtilizationRatio(), 0.0001);
    }

    @Test
    void computesSupplementaryIndicatorsDistributionCapacityInvariantsAndOperationOutcomes() {
        List<ReportingSourceData.EmergencyRow> emergencies = List.of(
                new ReportingSourceData.EmergencyRow(
                        "EMG-00001", "COMPLETED", "RUL-V01", "AMB-01", "HOSP-01", "COMPLETED",
                        1L, 1L, 2L, 4L, null, 4L, 7L, "HIGH", "INC-A"),
                new ReportingSourceData.EmergencyRow(
                        "EMG-00002", "WAITING_AMBULANCE", "RUL-V01", null, null, "NO_ELIGIBLE_AMBULANCE",
                        5L, 5L, null, null, null, null, null, "HIGH", "INC-A"),
                new ReportingSourceData.EmergencyRow(
                        "EMG-00003", "IN_TRANSPORT", "RUL-V01", "AMB-02", "HOSP-01", "RESERVED",
                        8L, 8L, 9L, 11L, null, 11L, null, "MEDIUM", "INC-B"),
                new ReportingSourceData.EmergencyRow(
                        "EMG-00004", "COMPLETED", "RUL-V01", "AMB-01", "HOSP-01", "COMPLETED",
                        12L, 12L, 13L, 14L, null, 14L, 16L, "LOW", "INC-D")
        );
        List<ReportingSourceData.HospitalRow> hospitals = List.of(
                // Válido y habilitado: C=5, R=1, O=2 -> D=2
                new ReportingSourceData.HospitalRow("HOSP-01", "Hospital Central", "N03", true, 5, 1, 2),
                // Deshabilitado: C=4, R=0, O=1 -> D=3 (no suma a enabledAvailable)
                new ReportingSourceData.HospitalRow("HOSP-02", "Hospital Cerrado", "N04", false, 4, 0, 1),
                // Inconsistente: C=2, R=2, O=1 -> R+O=3 > C -> invariantValid=false
                new ReportingSourceData.HospitalRow("HOSP-03", "Hospital Inconsistente", "N05", true, 2, 2, 1)
        );
        List<ReportingSourceData.OperationResultRow> results = List.of(
                new ReportingSourceData.OperationResultRow("OP-1", "AUTOMATIC_DISPATCH", "EMG-00001", "AMB-01", 1, "ASSIGNED", "OK", 2L),
                new ReportingSourceData.OperationResultRow("OP-2", "HOSPITAL_RESERVATION", "EMG-00001", "HOSP-01", 1, "IN_TRANSPORT", "OK", 4L),
                new ReportingSourceData.OperationResultRow("OP-3", "AUTOMATIC_DISPATCH", "EMG-00002", null, 1, "MOVED_TO_WAITING_AMBULANCE", "Cola", 5L),
                new ReportingSourceData.OperationResultRow("OP-4", "AUTOMATIC_DISPATCH", "EMG-00003", null, 3, "CONCURRENCY_RETRY_EXHAUSTED", "Agotado", 9L),
                new ReportingSourceData.OperationResultRow("OP-5", "HOSPITAL_RESERVATION", "EMG-00004", "HOSP-01", 1, "CUSTOM_STATUS", "Otro", 14L)
        );
        List<ReportingSourceData.AttemptLogRow> logs = List.of(
                new ReportingSourceData.AttemptLogRow("OP-1", "AUTOMATIC_DISPATCH", "EMG-00001", "AMB-01", 1, "ASSIGNED", "OK", 2L),
                new ReportingSourceData.AttemptLogRow("OP-4", "AUTOMATIC_DISPATCH", "EMG-00003", "AMB-02", 1, "AMBULANCE_UNAVAILABLE_CONFLICT", "C1", 9L),
                new ReportingSourceData.AttemptLogRow("OP-4", "AUTOMATIC_DISPATCH", "EMG-00003", "AMB-02", 2, "AMBULANCE_UNAVAILABLE_CONFLICT", "C2", 9L)
        );

        ReportingSourceData source = new ReportingSourceData(
                "SCN-P12", "ACTIVE", 20L,
                List.of(new ReportingSourceData.ScenarioRow("SCN-P12", "P1-2", "ACTIVE", 20L)),
                emergencies,
                List.of(new ReportingSourceData.AmbulanceRow("AMB-01", "BASIC", "AVAILABLE", null)),
                List.of(),
                results,
                logs,
                hospitals
        );

        SimulationReportSnapshot report = ReportingCalculator.calculateWithFilter(source, ReportingSupplementaryMetrics.TraceabilityFilter.none());

        // A. Distribución de emergencias
        ReportingSupplementaryMetrics.EmergencyDistributionSummary dist = report.emergencyDistribution();
        assertEquals(4L, dist.totalEmergencies());
        assertEquals(2L, dist.activeEmergencies());
        assertEquals(2L, dist.completedEmergencies());
        assertTrue(dist.totalsConsistent());
        assertEquals(2L, dist.byPriority().stream().filter(b -> b.key().equals("HIGH")).findFirst().orElseThrow().totalCount());
        assertEquals(1L, dist.byPriority().stream().filter(b -> b.key().equals("MEDIUM")).findFirst().orElseThrow().activeCount());
        assertEquals(1L, dist.byPriority().stream().filter(b -> b.key().equals("LOW")).findFirst().orElseThrow().completedCount());
        assertEquals(0L, dist.byCategory().stream().filter(b -> b.key().equals("INC-C")).findFirst().orElseThrow().totalCount());

        // B. Capacidad hospitalaria C/R/O/D e invariante
        ReportingSupplementaryMetrics.HospitalCapacitySummary cap = report.hospitalCapacity();
        assertEquals(3, cap.totalHospitals());
        assertEquals(2, cap.enabledHospitals());
        assertEquals(1, cap.disabledHospitals());
        assertEquals(11, cap.totalCapacity());
        assertEquals(3, cap.totalReserved());
        assertEquals(4, cap.totalOccupied());
        assertEquals(4, cap.totalAvailable());
        assertEquals(2, cap.enabledAvailable()); // HOSP-01 (+2) + HOSP-03 (inconsistente -1 -> 0); HOSP-02 deshabilitado excluido
        assertFalse(cap.allInvariantsValid());
        assertTrue(cap.items().get(0).invariantValid());
        assertTrue(cap.items().get(1).invariantValid());
        assertFalse(cap.items().get(2).invariantValid());

        // C. Resultados finales mutuamente excluyentes separados de intentos
        ReportingSupplementaryMetrics.OperationOutcomeSummary ops = report.operationOutcomes();
        assertEquals(5L, ops.totalFinalOperations());
        assertEquals(2L, ops.successfulCount());
        assertEquals(1L, ops.deferredToQueueCount());
        assertEquals(1L, ops.rejectedOrFailedCount());
        assertEquals(1L, ops.otherFinalStatusCount());
        assertEquals(ops.totalFinalOperations(),
                ops.successfulCount() + ops.deferredToQueueCount() + ops.rejectedOrFailedCount() + ops.otherFinalStatusCount());
        assertEquals(3L, ops.totalAttemptLogs());
        assertFalse(ops.emergencyOutcomeReasons().containsKey("COMPLETED"));
        assertEquals(1L, ops.emergencyOutcomeReasons().get("NO_ELIGIBLE_AMBULANCE"));

        // D. Filtros combinados (priority + rango temporal) afectan solo a experimentalRecords
        SimulationReportSnapshot filtered = ReportingCalculator.calculateWithFilter(
                source,
                new ReportingSupplementaryMetrics.TraceabilityFilter(null, "HIGH", 4L, 10L)
        );
        assertEquals(1, filtered.experimentalRecords().size());
        assertEquals("EMG-00002", filtered.experimentalRecords().getFirst().emergencyId());
        // Métricas globales y distribución global permanecen intactas
        assertEquals(2L, filtered.completedEmergencies().completedCount());
        assertEquals(4L, filtered.emergencyDistribution().totalEmergencies());
    }

    @Test
    void rejectsInvalidTraceabilityFilterRanges() {
        assertThrows(IllegalArgumentException.class,
                () -> new ReportingSupplementaryMetrics.TraceabilityFilter(null, null, -1L, 5L));
        assertThrows(IllegalArgumentException.class,
                () -> new ReportingSupplementaryMetrics.TraceabilityFilter(null, null, 0L, -2L));
        assertThrows(IllegalArgumentException.class,
                () -> new ReportingSupplementaryMetrics.TraceabilityFilter(null, null, 10L, 5L));
    }
}

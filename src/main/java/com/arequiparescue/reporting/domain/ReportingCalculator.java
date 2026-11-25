package com.arequiparescue.reporting.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Calculadora determinista de dominio para las 7 métricas experimentales ({@code RN-MET-004})
 * y el registro funcional trazable por emergencia ({@code RN-MET-003}) sin efectos secundarios.
 */
public final class ReportingCalculator {

    private static final String OP_HOSPITAL_RESERVATION = "HOSPITAL_RESERVATION";
    private static final String OP_WAITING_HOSPITAL_QUEUE = "WAITING_HOSPITAL_QUEUE";
    private static final List<String> PRIORITY_ORDER = List.of("HIGH", "MEDIUM", "LOW");
    private static final List<String> CATEGORY_ORDER = List.of("INC-A", "INC-B", "INC-C", "INC-D");
    private static final List<String> ACTIVE_STATES_ORDER = List.of(
            "REGISTERED",
            "WAITING_AMBULANCE",
            "ASSIGNED",
            "AT_INCIDENT",
            "WAITING_HOSPITAL",
            "IN_TRANSPORT"
    );

    private ReportingCalculator() {
    }

    public static SimulationReportSnapshot calculate(ReportingSourceData source, String stateFilter) {
        return calculateWithFilter(source, new ReportingSupplementaryMetrics.TraceabilityFilter(stateFilter, null, null, null));
    }

    public static SimulationReportSnapshot calculateWithFilter(
            ReportingSourceData source,
            ReportingSupplementaryMetrics.TraceabilityFilter filter
    ) {
        Objects.requireNonNull(source, "Los datos fuente de reporte no pueden ser nulos.");
        ReportingSupplementaryMetrics.TraceabilityFilter effectiveFilter =
                filter != null ? filter : ReportingSupplementaryMetrics.TraceabilityFilter.none();
        long currentTicks = Math.max(0L, source.currentLogicalTicks());

        List<ReportingSourceData.EmergencyRow> emergencies = source.emergencies().stream()
                .sorted(Comparator.comparing(ReportingSourceData.EmergencyRow::id))
                .toList();

        TemporalMetricSummary waitingForAmbulance = computeWaitingForAmbulance(emergencies, currentTicks);
        TemporalMetricSummary responseToIncident = computeResponseToIncident(emergencies, currentTicks);
        TemporalMetricSummary pickupToHospital = computePickupToHospital(emergencies, currentTicks);
        CompletedEmergenciesMetric completedEmergencies = computeCompletedEmergencies(emergencies);
        AmbulanceUtilizationMetric ambulanceUtilization =
                computeAmbulanceUtilization(source.ambulances(), emergencies, currentTicks);
        ConcurrencyMetric concurrencyConflicts =
                computeConcurrencyMetric(source.operationResults(), source.attemptLogs());
        TemporalMetricSummary waitingForHospital = computeWaitingForHospital(emergencies, currentTicks);

        ReportingSupplementaryMetrics.EmergencyDistributionSummary emergencyDistribution =
                computeEmergencyDistribution(emergencies);
        ReportingSupplementaryMetrics.HospitalCapacitySummary hospitalCapacity =
                computeHospitalCapacity(source.hospitals());
        ReportingSupplementaryMetrics.OperationOutcomeSummary operationOutcomes =
                computeOperationOutcomes(emergencies, source.operationResults(), source.attemptLogs());

        List<ExperimentalEmergencyRecord> records = buildExperimentalRecords(
                emergencies,
                source.hospitalReservations(),
                source.operationResults(),
                source.attemptLogs(),
                effectiveFilter
        );

        return new SimulationReportSnapshot(
                source.referenceScenarioId(),
                source.referenceScenarioStatus(),
                currentTicks,
                waitingForAmbulance,
                responseToIncident,
                pickupToHospital,
                completedEmergencies,
                ambulanceUtilization,
                concurrencyConflicts,
                waitingForHospital,
                records,
                emergencyDistribution,
                hospitalCapacity,
                operationOutcomes
        );
    }

    private static TemporalMetricSummary computeWaitingForAmbulance(
            List<ReportingSourceData.EmergencyRow> emergencies,
            long currentTicks
    ) {
        List<Long> closedDurations = new ArrayList<>();
        List<TemporalMetricSummary.InProgressTemporalItem> inProgress = new ArrayList<>();

        for (ReportingSourceData.EmergencyRow emg : emergencies) {
            long startTicks = emg.queueEnteredAtTicks() != null
                    ? emg.queueEnteredAtTicks()
                    : emg.registeredAtTicks();
            if (emg.assignedAtTicks() != null) {
                closedDurations.add(Math.max(0L, emg.assignedAtTicks() - startTicks));
            } else {
                inProgress.add(new TemporalMetricSummary.InProgressTemporalItem(
                        emg.id(),
                        startTicks,
                        Math.max(0L, currentTicks - startTicks)
                ));
            }
        }

        return new TemporalMetricSummary(
                "MET-01",
                "Tiempo de espera hasta asignación de ambulancia",
                "unidades de tiempo lógico",
                closedDurations.size(),
                averageOrNull(closedDurations),
                inProgress.size(),
                "EN_ESPERA",
                inProgress
        );
    }

    private static TemporalMetricSummary computeResponseToIncident(
            List<ReportingSourceData.EmergencyRow> emergencies,
            long currentTicks
    ) {
        List<Long> closedDurations = new ArrayList<>();
        List<TemporalMetricSummary.InProgressTemporalItem> inProgress = new ArrayList<>();

        for (ReportingSourceData.EmergencyRow emg : emergencies) {
            long startTicks = emg.registeredAtTicks();
            if (emg.atIncidentAtTicks() != null) {
                closedDurations.add(Math.max(0L, emg.atIncidentAtTicks() - startTicks));
            } else {
                inProgress.add(new TemporalMetricSummary.InProgressTemporalItem(
                        emg.id(),
                        startTicks,
                        Math.max(0L, currentTicks - startTicks)
                ));
            }
        }

        return new TemporalMetricSummary(
                "MET-02",
                "Tiempo de respuesta hasta llegada al incidente",
                "unidades de tiempo lógico",
                closedDurations.size(),
                averageOrNull(closedDurations),
                inProgress.size(),
                "PENDIENTE_LLEGADA_INCIDENTE",
                inProgress
        );
    }

    private static TemporalMetricSummary computePickupToHospital(
            List<ReportingSourceData.EmergencyRow> emergencies,
            long currentTicks
    ) {
        List<Long> closedDurations = new ArrayList<>();
        List<TemporalMetricSummary.InProgressTemporalItem> inProgress = new ArrayList<>();

        for (ReportingSourceData.EmergencyRow emg : emergencies) {
            if (emg.atIncidentAtTicks() == null) {
                continue;
            }
            long startTicks = emg.atIncidentAtTicks();
            if (emg.completedAtTicks() != null) {
                closedDurations.add(Math.max(0L, emg.completedAtTicks() - startTicks));
            } else {
                inProgress.add(new TemporalMetricSummary.InProgressTemporalItem(
                        emg.id(),
                        startTicks,
                        Math.max(0L, currentTicks - startTicks)
                ));
            }
        }

        return new TemporalMetricSummary(
                "MET-03",
                "Tiempo desde recogida hasta llegada al hospital",
                "unidades de tiempo lógico",
                closedDurations.size(),
                averageOrNull(closedDurations),
                inProgress.size(),
                "EN_PROCESO_TRASLADO",
                inProgress
        );
    }

    private static CompletedEmergenciesMetric computeCompletedEmergencies(
            List<ReportingSourceData.EmergencyRow> emergencies
    ) {
        long completedCount = 0L;
        long activeCount = 0L;
        Map<String, Long> activeCountsByState = new LinkedHashMap<>();
        for (String state : ACTIVE_STATES_ORDER) {
            activeCountsByState.put(state, 0L);
        }

        for (ReportingSourceData.EmergencyRow emg : emergencies) {
            if ("COMPLETED".equals(emg.state())) {
                completedCount++;
            } else {
                activeCount++;
                activeCountsByState.merge(emg.state(), 1L, Long::sum);
            }
        }

        return new CompletedEmergenciesMetric(completedCount, activeCount, activeCountsByState);
    }

    private static AmbulanceUtilizationMetric computeAmbulanceUtilization(
            List<ReportingSourceData.AmbulanceRow> ambulances,
            List<ReportingSourceData.EmergencyRow> emergencies,
            long currentTicks
    ) {
        Map<String, ReportingSourceData.EmergencyRow> emergencyById = emergencies.stream()
                .collect(Collectors.toMap(ReportingSourceData.EmergencyRow::id, row -> row));

        Map<String, Long> completedOccupiedByAmbulance = new LinkedHashMap<>();
        for (ReportingSourceData.EmergencyRow emg : emergencies) {
            if (emg.assignedAmbulanceId() != null
                    && emg.assignedAtTicks() != null
                    && emg.completedAtTicks() != null) {
                long duration = Math.max(0L, emg.completedAtTicks() - emg.assignedAtTicks());
                completedOccupiedByAmbulance.merge(emg.assignedAmbulanceId(), duration, Long::sum);
            }
        }

        List<ReportingSourceData.AmbulanceRow> sortedAmbulances = ambulances.stream()
                .sorted(Comparator.comparing(ReportingSourceData.AmbulanceRow::id))
                .toList();

        List<AmbulanceUtilizationMetric.AmbulanceUtilizationItem> items = new ArrayList<>();
        long totalOccupiedTicks = 0L;

        for (ReportingSourceData.AmbulanceRow amb : sortedAmbulances) {
            long occupied = completedOccupiedByAmbulance.getOrDefault(amb.id(), 0L);
            if (!"AVAILABLE".equals(amb.state()) && amb.assignedEmergencyId() != null) {
                ReportingSourceData.EmergencyRow activeEmergency = emergencyById.get(amb.assignedEmergencyId());
                if (activeEmergency != null && activeEmergency.assignedAtTicks() != null) {
                    occupied += Math.max(0L, currentTicks - activeEmergency.assignedAtTicks());
                }
            }
            totalOccupiedTicks += occupied;
            double ratio = currentTicks > 0L ? (double) occupied / (double) currentTicks : 0.0;
            items.add(new AmbulanceUtilizationMetric.AmbulanceUtilizationItem(
                    amb.id(),
                    amb.equipmentType(),
                    amb.state(),
                    occupied,
                    ratio
            ));
        }

        double averageRatio = (sortedAmbulances.isEmpty() || currentTicks <= 0L)
                ? 0.0
                : (double) totalOccupiedTicks / ((double) sortedAmbulances.size() * (double) currentTicks);

        return new AmbulanceUtilizationMetric(currentTicks, totalOccupiedTicks, averageRatio, items);
    }

    private static ConcurrencyMetric computeConcurrencyMetric(
            List<ReportingSourceData.OperationResultRow> operationResults,
            List<ReportingSourceData.AttemptLogRow> attemptLogs
    ) {
        long dispatchConflicts = 0L;
        long dispatchRetries = 0L;
        long dispatchExhaustions = 0L;

        long hospitalConflicts = 0L;
        long hospitalRetries = 0L;
        long hospitalExhaustions = 0L;

        for (ReportingSourceData.AttemptLogRow log : attemptLogs) {
            boolean isHospital = isHospitalOperation(log.operationType());
            if (isHospital) {
                if ("HOSPITAL_CAPACITY_CONFLICT".equals(log.outcomeCode())) {
                    hospitalConflicts++;
                }
                if (log.attemptNumber() > 1) {
                    hospitalRetries++;
                }
            } else {
                if ("AMBULANCE_UNAVAILABLE_CONFLICT".equals(log.outcomeCode())) {
                    dispatchConflicts++;
                }
                if (log.attemptNumber() > 1) {
                    dispatchRetries++;
                }
            }
        }

        for (ReportingSourceData.OperationResultRow res : operationResults) {
            if (isHospitalOperation(res.operationType())) {
                if ("HOSPITAL_RESERVATION_RETRY_EXHAUSTED".equals(res.finalStatus())) {
                    hospitalExhaustions++;
                }
            } else if ("CONCURRENCY_RETRY_EXHAUSTED".equals(res.finalStatus())) {
                dispatchExhaustions++;
            }
        }

        ConcurrencyMetric.OperationBreakdown dispatchBreakdown =
                new ConcurrencyMetric.OperationBreakdown(dispatchConflicts, dispatchRetries, dispatchExhaustions);
        ConcurrencyMetric.OperationBreakdown hospitalBreakdown =
                new ConcurrencyMetric.OperationBreakdown(hospitalConflicts, hospitalRetries, hospitalExhaustions);

        return new ConcurrencyMetric(
                dispatchBreakdown,
                hospitalBreakdown,
                dispatchConflicts + hospitalConflicts,
                dispatchRetries + hospitalRetries,
                dispatchExhaustions + hospitalExhaustions
        );
    }

    private static boolean isHospitalOperation(String operationType) {
        return OP_HOSPITAL_RESERVATION.equals(operationType)
                || OP_WAITING_HOSPITAL_QUEUE.equals(operationType);
    }

    private static TemporalMetricSummary computeWaitingForHospital(
            List<ReportingSourceData.EmergencyRow> emergencies,
            long currentTicks
    ) {
        List<Long> closedDurations = new ArrayList<>();
        List<TemporalMetricSummary.InProgressTemporalItem> inProgress = new ArrayList<>();

        for (ReportingSourceData.EmergencyRow emg : emergencies) {
            if (emg.inTransportAtTicks() != null) {
                long duration = emg.hospitalQueueEnteredAtTicks() != null
                        ? Math.max(0L, emg.inTransportAtTicks() - emg.hospitalQueueEnteredAtTicks())
                        : 0L;
                closedDurations.add(duration);
            } else if ("WAITING_HOSPITAL".equals(emg.state())) {
                long startTicks = emg.hospitalQueueEnteredAtTicks() != null
                        ? emg.hospitalQueueEnteredAtTicks()
                        : (emg.atIncidentAtTicks() != null ? emg.atIncidentAtTicks() : emg.registeredAtTicks());
                inProgress.add(new TemporalMetricSummary.InProgressTemporalItem(
                        emg.id(),
                        startTicks,
                        Math.max(0L, currentTicks - startTicks)
                ));
            }
        }

        return new TemporalMetricSummary(
                "MET-07",
                "Tiempo de espera por destino hospitalario",
                "unidades de tiempo lógico",
                closedDurations.size(),
                averageOrNull(closedDurations),
                inProgress.size(),
                "EN_ESPERA_HOSPITAL",
                inProgress
        );
    }

    private static List<ExperimentalEmergencyRecord> buildExperimentalRecords(
            List<ReportingSourceData.EmergencyRow> emergencies,
            List<ReportingSourceData.HospitalReservationRow> reservations,
            List<ReportingSourceData.OperationResultRow> operationResults,
            List<ReportingSourceData.AttemptLogRow> attemptLogs,
            ReportingSupplementaryMetrics.TraceabilityFilter filter
    ) {
        Map<String, ReportingSourceData.HospitalReservationRow> reservationByEmergency = reservations.stream()
                .collect(Collectors.toMap(
                        ReportingSourceData.HospitalReservationRow::emergencyId,
                        row -> row,
                        (first, second) -> second
                ));

        Map<String, List<ExperimentalEmergencyRecord.OperationResultTrace>> resultsByEmergency = new LinkedHashMap<>();
        for (ReportingSourceData.OperationResultRow res : operationResults) {
            resultsByEmergency.computeIfAbsent(res.emergencyId(), key -> new ArrayList<>())
                    .add(new ExperimentalEmergencyRecord.OperationResultTrace(
                            res.operationId(),
                            res.operationType(),
                            res.assignedResourceId(),
                            res.totalAttempts(),
                            res.finalStatus(),
                            res.outcomeReason(),
                            res.logicalTicks()
                    ));
        }

        Map<String, List<ExperimentalEmergencyRecord.AttemptLogTrace>> attemptsByEmergency = new LinkedHashMap<>();
        for (ReportingSourceData.AttemptLogRow log : attemptLogs) {
            attemptsByEmergency.computeIfAbsent(log.emergencyId(), key -> new ArrayList<>())
                    .add(new ExperimentalEmergencyRecord.AttemptLogTrace(
                            log.operationId(),
                            log.operationType(),
                            log.candidateResourceId(),
                            log.attemptNumber(),
                            log.outcomeCode(),
                            log.detailMessage(),
                            log.logicalTicks()
                    ));
        }

        List<ExperimentalEmergencyRecord> records = new ArrayList<>();
        for (ReportingSourceData.EmergencyRow emg : emergencies) {
            if (filter.state() != null && !filter.state().equals(emg.state())) {
                continue;
            }
            if (filter.priority() != null && !filter.priority().equals(emg.simulatedPriority())) {
                continue;
            }
            if (filter.fromTicks() != null && emg.registeredAtTicks() < filter.fromTicks()) {
                continue;
            }
            if (filter.toTicks() != null && emg.registeredAtTicks() > filter.toTicks()) {
                continue;
            }
            ReportingSourceData.HospitalReservationRow reservation = reservationByEmergency.get(emg.id());
            String hospitalId = emg.hospitalId() != null
                    ? emg.hospitalId()
                    : (reservation != null ? reservation.hospitalId() : null);
            Long dischargedAt = reservation != null ? reservation.dischargedAtTicks() : null;

            records.add(new ExperimentalEmergencyRecord(
                    emg.id(),
                    emg.state(),
                    emg.ruleVersionId(),
                    emg.assignedAmbulanceId(),
                    hospitalId,
                    emg.lastOutcomeReason(),
                    emg.registeredAtTicks(),
                    emg.queueEnteredAtTicks(),
                    emg.assignedAtTicks(),
                    emg.atIncidentAtTicks(),
                    emg.hospitalQueueEnteredAtTicks(),
                    emg.inTransportAtTicks(),
                    emg.completedAtTicks(),
                    dischargedAt,
                    List.copyOf(resultsByEmergency.getOrDefault(emg.id(), List.of())),
                    List.copyOf(attemptsByEmergency.getOrDefault(emg.id(), List.of())),
                    emg.simulatedPriority(),
                    emg.incidentCategory()
            ));
        }
        return List.copyOf(records);
    }

    private static ReportingSupplementaryMetrics.EmergencyDistributionSummary computeEmergencyDistribution(
            List<ReportingSourceData.EmergencyRow> emergencies
    ) {
        Map<String, long[]> priorityMap = new LinkedHashMap<>();
        for (String p : PRIORITY_ORDER) {
            priorityMap.put(p, new long[2]); // [0] = active, [1] = completed
        }
        Map<String, long[]> categoryMap = new LinkedHashMap<>();
        for (String c : CATEGORY_ORDER) {
            categoryMap.put(c, new long[2]);
        }

        long activeTotal = 0L;
        long completedTotal = 0L;
        for (ReportingSourceData.EmergencyRow emg : emergencies) {
            boolean completed = "COMPLETED".equals(emg.state());
            if (completed) {
                completedTotal++;
            } else {
                activeTotal++;
            }
            int idx = completed ? 1 : 0;
            String prioKey = (emg.simulatedPriority() != null && !emg.simulatedPriority().isBlank())
                    ? emg.simulatedPriority().trim()
                    : "SIN_PRIORIDAD";
            priorityMap.computeIfAbsent(prioKey, k -> new long[2])[idx]++;

            String catKey = (emg.incidentCategory() != null && !emg.incidentCategory().isBlank())
                    ? emg.incidentCategory().trim()
                    : "SIN_CATEGORIA";
            categoryMap.computeIfAbsent(catKey, k -> new long[2])[idx]++;
        }

        List<ReportingSupplementaryMetrics.DistributionBucket> byPriority = priorityMap.entrySet().stream()
                .map(e -> new ReportingSupplementaryMetrics.DistributionBucket(
                        e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[0] + e.getValue()[1]))
                .toList();
        List<ReportingSupplementaryMetrics.DistributionBucket> byCategory = categoryMap.entrySet().stream()
                .map(e -> new ReportingSupplementaryMetrics.DistributionBucket(
                        e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[0] + e.getValue()[1]))
                .toList();

        long total = emergencies.size();
        long sumPrio = byPriority.stream().mapToLong(ReportingSupplementaryMetrics.DistributionBucket::totalCount).sum();
        long sumCat = byCategory.stream().mapToLong(ReportingSupplementaryMetrics.DistributionBucket::totalCount).sum();
        boolean consistent = (activeTotal + completedTotal == total) && (sumPrio == total) && (sumCat == total);

        return new ReportingSupplementaryMetrics.EmergencyDistributionSummary(
                total, activeTotal, completedTotal, byPriority, byCategory, consistent);
    }

    private static ReportingSupplementaryMetrics.HospitalCapacitySummary computeHospitalCapacity(
            List<ReportingSourceData.HospitalRow> hospitals
    ) {
        if (hospitals == null || hospitals.isEmpty()) {
            return ReportingSupplementaryMetrics.HospitalCapacitySummary.empty();
        }
        List<ReportingSourceData.HospitalRow> sorted = hospitals.stream()
                .sorted(Comparator.comparing(ReportingSourceData.HospitalRow::id))
                .toList();

        int enabledCount = 0;
        int disabledCount = 0;
        int totalC = 0;
        int totalR = 0;
        int totalO = 0;
        int totalD = 0;
        int enabledD = 0;
        boolean allValid = true;

        List<ReportingSupplementaryMetrics.HospitalCapacityItem> items = new ArrayList<>();
        for (ReportingSourceData.HospitalRow h : sorted) {
            int available = h.capacityTotal() - h.reservedSlots() - h.occupiedSlots();
            boolean valid = h.capacityTotal() >= 0
                    && h.reservedSlots() >= 0
                    && h.occupiedSlots() >= 0
                    && available >= 0
                    && (h.reservedSlots() + h.occupiedSlots() + available == h.capacityTotal());
            if (!valid) {
                allValid = false;
            }
            if (h.enabled()) {
                enabledCount++;
                enabledD += Math.max(0, available);
            } else {
                disabledCount++;
            }
            totalC += h.capacityTotal();
            totalR += h.reservedSlots();
            totalO += h.occupiedSlots();
            totalD += available;

            items.add(new ReportingSupplementaryMetrics.HospitalCapacityItem(
                    h.id(), h.name(), h.nodeId(), h.enabled(),
                    h.capacityTotal(), h.reservedSlots(), h.occupiedSlots(), available, valid));
        }

        return new ReportingSupplementaryMetrics.HospitalCapacitySummary(
                sorted.size(), enabledCount, disabledCount,
                totalC, totalR, totalO, totalD, enabledD, allValid, List.copyOf(items));
    }

    private static ReportingSupplementaryMetrics.OperationOutcomeSummary computeOperationOutcomes(
            List<ReportingSourceData.EmergencyRow> emergencies,
            List<ReportingSourceData.OperationResultRow> operationResults,
            List<ReportingSourceData.AttemptLogRow> attemptLogs
    ) {
        long successful = 0L;
        long deferred = 0L;
        long rejected = 0L;
        long other = 0L;
        Map<String, Long> finalStatusBreakdown = new LinkedHashMap<>();

        for (ReportingSourceData.OperationResultRow res : operationResults) {
            String status = res.finalStatus() != null ? res.finalStatus().trim() : "UNKNOWN";
            finalStatusBreakdown.merge(status, 1L, Long::sum);
            switch (status) {
                case "ASSIGNED", "IN_TRANSPORT", "COMPLETED", "DISCHARGED" -> successful++;
                case "MOVED_TO_WAITING_AMBULANCE", "MOVED_TO_WAITING_HOSPITAL" -> deferred++;
                case "DUPLICATE_OR_INVALID_STATE",
                        "CONFLICT_RELOAD_REQUIRED",
                        "CONCURRENCY_RETRY_EXHAUSTED",
                        "HOSPITAL_RESERVATION_RETRY_EXHAUSTED" -> rejected++;
                default -> other++;
            }
        }

        Map<String, Long> outcomeReasons = new LinkedHashMap<>();
        for (ReportingSourceData.EmergencyRow emg : emergencies) {
            if (emg.lastOutcomeReason() != null && !emg.lastOutcomeReason().isBlank()) {
                String reason = emg.lastOutcomeReason().trim();
                if (!"HOSPITAL_DELIVERY_COMPLETED".equals(reason) && !"COMPLETED".equals(reason)) {
                    outcomeReasons.merge(reason, 1L, Long::sum);
                }
            }
        }

        return new ReportingSupplementaryMetrics.OperationOutcomeSummary(
                operationResults.size(),
                successful,
                deferred,
                rejected,
                other,
                attemptLogs.size(),
                Map.copyOf(finalStatusBreakdown),
                Map.copyOf(outcomeReasons)
        );
    }

    private static Double averageOrNull(List<Long> values) {
        if (values.isEmpty()) {
            return null;
        }
        long sum = 0L;
        for (long value : values) {
            sum += value;
        }
        return (double) sum / (double) values.size();
    }
}

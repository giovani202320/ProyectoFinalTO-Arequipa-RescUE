package com.arequiparescue.dispatch.application;

import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.ambulance.domain.AmbulanceId;
import com.arequiparescue.ambulance.domain.AmbulanceState;
import com.arequiparescue.ambulance.domain.port.AmbulanceRepository;
import com.arequiparescue.dispatch.domain.AmbulanceEligibilityPolicy;
import com.arequiparescue.dispatch.domain.DispatchOperationOutcome;
import com.arequiparescue.dispatch.domain.EligibleAmbulanceCandidate;
import com.arequiparescue.dispatch.domain.WaitingAmbulanceQueuePolicy;
import com.arequiparescue.dispatch.domain.port.DispatchTraceabilityRepository;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyId;
import com.arequiparescue.emergency.domain.EmergencyState;
import com.arequiparescue.emergency.domain.port.EmergencyRepository;
import com.arequiparescue.routing.domain.RoadGraph;
import com.arequiparescue.routing.domain.port.RoadGraphRepository;
import com.arequiparescue.shared.domain.LogicalClock;
import com.arequiparescue.shared.domain.LogicalTime;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Coordinador de aplicación para evaluación de ambulancias elegibles (HU-05), asignación
 * supervisada (HU-06), asignación automática con reintentos acotados 1+3 (HU-07) y atención
 * FIFO de la cola {@code WAITING_AMBULANCE} (HU-08), conforme a ADR-0002.
 */
@Service
public class DispatchApplicationService {

    public static final int MAX_AUTOMATIC_ATTEMPTS = 4;

    private static final String OP_SUPERVISED = "SUPERVISED_DISPATCH";
    private static final String OP_AUTOMATIC = "AUTOMATIC_DISPATCH";
    private static final String OP_WAITING_QUEUE = "WAITING_QUEUE_DISPATCH";

    private final EmergencyRepository emergencyRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final RoadGraphRepository roadGraphRepository;
    private final DispatchTraceabilityRepository traceabilityRepository;
    private final SupervisedDispatchAttemptExecutor attemptExecutor;
    private final LogicalClock logicalClock;
    private final AmbulanceEligibilityPolicy eligibilityPolicy;
    private final WaitingAmbulanceQueuePolicy waitingQueuePolicy;

    public DispatchApplicationService(
            EmergencyRepository emergencyRepository,
            AmbulanceRepository ambulanceRepository,
            RoadGraphRepository roadGraphRepository,
            DispatchTraceabilityRepository traceabilityRepository,
            SupervisedDispatchAttemptExecutor attemptExecutor,
            LogicalClock logicalClock
    ) {
        this.emergencyRepository = Objects.requireNonNull(emergencyRepository);
        this.ambulanceRepository = Objects.requireNonNull(ambulanceRepository);
        this.roadGraphRepository = Objects.requireNonNull(roadGraphRepository);
        this.traceabilityRepository = Objects.requireNonNull(traceabilityRepository);
        this.attemptExecutor = Objects.requireNonNull(attemptExecutor);
        this.logicalClock = Objects.requireNonNull(logicalClock);
        this.eligibilityPolicy = new AmbulanceEligibilityPolicy();
        this.waitingQueuePolicy = new WaitingAmbulanceQueuePolicy();
    }

    public record EligibleEvaluation(
            Emergency emergency,
            List<EligibleAmbulanceCandidate> candidates
    ) {
    }

    @Transactional(readOnly = true)
    public EligibleEvaluation evaluateEligibleAmbulances(String rawEmergencyId) {
        EmergencyId emergencyId = parseEmergencyId(rawEmergencyId);
        Emergency emergency = emergencyRepository.findById(emergencyId)
                .orElseThrow(() -> new DispatchValidationException(
                        "No existe ninguna emergencia con identificador " + emergencyId.value() + "."
                ));
        if (emergency.state() != EmergencyState.REGISTERED
                && emergency.state() != EmergencyState.WAITING_AMBULANCE) {
            throw new DispatchValidationException(
                    "Solo se pueden consultar ambulancias elegibles para emergencias en REGISTERED o WAITING_AMBULANCE. Estado actual: "
                            + emergency.state());
        }
        List<Ambulance> catalog = ambulanceRepository.findAllOrderById();
        RoadGraph graph = roadGraphRepository.loadGraph();
        List<EligibleAmbulanceCandidate> candidates =
                eligibilityPolicy.evaluateEligibleCandidates(emergency, catalog, graph);
        return new EligibleEvaluation(emergency, candidates);
    }

    /**
     * Ejecuta la asignación supervisada (HU-06) en intento único (maxAttempts = 1, RN-CON-005).
     * Ante conflicto concurrente recarga las alternativas elegibles o transiciona a WAITING_AMBULANCE.
     */
    public SupervisedDispatchResult assignSupervised(String rawEmergencyId, String rawAmbulanceId) {
        EmergencyId emergencyId = parseEmergencyId(rawEmergencyId);
        AmbulanceId ambulanceId = parseAmbulanceId(rawAmbulanceId);
        String operationId = "SUP-" + UUID.randomUUID();
        LogicalTime now = logicalClock.currentTime();

        SupervisedDispatchAttemptExecutor.AttemptOutcome attemptOutcome;
        try {
            attemptOutcome = attemptExecutor.executeSupervisedAssignmentAttempt(emergencyId, ambulanceId, now);
        } catch (DispatchValidationException ex) {
            throw ex;
        } catch (PessimisticLockingFailureException ex) {
            Emergency current = emergencyRepository.findById(emergencyId)
                    .orElseThrow(() -> new DispatchValidationException(
                            "No existe ninguna emergencia con identificador " + emergencyId.value() + "."));
            attemptOutcome = new SupervisedDispatchAttemptExecutor.AttemptOutcome(
                    false,
                    true,
                    current,
                    null,
                    "Conflicto concurrente de bloqueo al intentar asignar la ambulancia " + ambulanceId.value() + "."
            );
        } catch (RuntimeException ex) {
            // Error técnico ajeno a conflicto concurrente (incl. DataIntegrityViolationException): no se clasifica como conflicto (ADR-0002 Decisión 4)
            recordTechnicalErrorBestEffort(operationId, OP_SUPERVISED, emergencyId.value(),
                    ambulanceId.value(), 1, ex.getMessage(), now.ticks());
            throw ex;
        }

        if (attemptOutcome.succeeded()) {
            traceabilityRepository.recordAttempt(
                    operationId,
                    OP_SUPERVISED,
                    emergencyId.value(),
                    ambulanceId.value(),
                    1,
                    "ASSIGNED",
                    attemptOutcome.reason(),
                    now.ticks()
            );
            traceabilityRepository.recordOperationResult(
                    operationId,
                    OP_SUPERVISED,
                    emergencyId.value(),
                    ambulanceId.value(),
                    1,
                    "ASSIGNED",
                    attemptOutcome.reason(),
                    now.ticks()
            );
            return new SupervisedDispatchResult(
                    operationId,
                    DispatchOperationOutcome.ASSIGNED,
                    attemptOutcome.emergency(),
                    attemptOutcome.ambulance(),
                    List.of(),
                    attemptOutcome.reason()
            );
        }

        // Conflicto en modalidad supervisada: no reintentar automáticamente (RN-CON-002, RN-CON-005)
        traceabilityRepository.recordAttempt(
                operationId,
                OP_SUPERVISED,
                emergencyId.value(),
                ambulanceId.value(),
                1,
                "AMBULANCE_UNAVAILABLE_CONFLICT",
                attemptOutcome.reason(),
                now.ticks()
        );

        Emergency freshEmergency = emergencyRepository.findById(emergencyId)
                .orElse(attemptOutcome.emergency());
        List<EligibleAmbulanceCandidate> refreshed = List.of();
        if (freshEmergency.state() == EmergencyState.REGISTERED
                || freshEmergency.state() == EmergencyState.WAITING_AMBULANCE) {
            refreshed = eligibilityPolicy.evaluateEligibleCandidates(
                    freshEmergency,
                    ambulanceRepository.findAllOrderById(),
                    roadGraphRepository.loadGraph()
            );
        }

        if (!refreshed.isEmpty()) {
            String message = attemptOutcome.reason()
                    + " Seleccione manualmente otra ambulancia de la lista actualizada.";
            traceabilityRepository.recordOperationResult(
                    operationId,
                    OP_SUPERVISED,
                    emergencyId.value(),
                    null,
                    1,
                    "CONFLICT_RELOAD_REQUIRED",
                    message,
                    now.ticks()
            );
            return new SupervisedDispatchResult(
                    operationId,
                    DispatchOperationOutcome.CONFLICT_RELOAD_REQUIRED,
                    freshEmergency,
                    null,
                    refreshed,
                    message
            );
        }

        String waitingReason = attemptOutcome.reason()
                + " No quedan ambulancias elegibles disponibles; la emergencia pasa/permanece en WAITING_AMBULANCE.";
        Emergency waitingEmergency = attemptExecutor.moveEmergencyToWaitingAmbulance(
                emergencyId,
                now,
                "SUPERVISED_CONFLICT_NO_ELIGIBLE"
        );
        traceabilityRepository.recordOperationResult(
                operationId,
                OP_SUPERVISED,
                emergencyId.value(),
                null,
                1,
                "MOVED_TO_WAITING_AMBULANCE",
                waitingReason,
                now.ticks()
        );
        return new SupervisedDispatchResult(
                operationId,
                DispatchOperationOutcome.MOVED_TO_WAITING_AMBULANCE,
                waitingEmergency,
                null,
                List.of(),
                waitingReason
        );
    }

    /**
     * Registra la permanencia o transición a WAITING_AMBULANCE (T-EME-02) cuando al evaluar no
     * existen ambulancias elegibles disponibles (RN-ASI-004, HU-06 CA-3).
     */
    public SupervisedDispatchResult moveToWaitingWhenNoEligible(String rawEmergencyId) {
        EmergencyId emergencyId = parseEmergencyId(rawEmergencyId);
        EligibleEvaluation evaluation = evaluateEligibleAmbulances(emergencyId.value());
        if (!evaluation.candidates().isEmpty()) {
            return new SupervisedDispatchResult(
                    "EVAL-" + UUID.randomUUID(),
                    DispatchOperationOutcome.CONFLICT_RELOAD_REQUIRED,
                    evaluation.emergency(),
                    null,
                    evaluation.candidates(),
                    "Existen ambulancias elegibles disponibles para selección supervisada."
            );
        }

        String operationId = "WAIT-" + UUID.randomUUID();
        LogicalTime now = logicalClock.currentTime();
        String reason = "Sin ambulancias elegibles disponibles/alcanzables; la emergencia queda en WAITING_AMBULANCE (RN-ASI-004).";
        Emergency waiting = attemptExecutor.moveEmergencyToWaitingAmbulance(
                emergencyId,
                now,
                "NO_ELIGIBLE_AMBULANCE"
        );
        traceabilityRepository.recordOperationResult(
                operationId,
                OP_SUPERVISED,
                emergencyId.value(),
                null,
                0,
                "MOVED_TO_WAITING_AMBULANCE",
                reason,
                now.ticks()
        );
        return new SupervisedDispatchResult(
                operationId,
                DispatchOperationOutcome.MOVED_TO_WAITING_AMBULANCE,
                waiting,
                null,
                List.of(),
                reason
        );
    }

    /**
     * Ejecuta la asignación automática de ambulancia (HU-07, DEC-017, ADR-0002):
     * 1 intento inicial + hasta 3 reintentos adicionales (máximo 4 intentos) con reevaluación
     * integral de elegibilidad, preservación, Dijkstra y desempate antes de cada intento.
     */
    public AutomaticDispatchResult assignAutomatic(String rawEmergencyId) {
        EmergencyId emergencyId = parseEmergencyId(rawEmergencyId);
        Emergency initialEmergency = emergencyRepository.findById(emergencyId)
                .orElseThrow(() -> new DispatchValidationException(
                        "No existe ninguna emergencia con identificador " + emergencyId.value() + "."));
        if (initialEmergency.state() != EmergencyState.REGISTERED
                && initialEmergency.state() != EmergencyState.WAITING_AMBULANCE) {
            throw new DispatchValidationException(
                    "Solo se puede ejecutar asignación automática para emergencias en REGISTERED o WAITING_AMBULANCE. Estado actual: "
                            + initialEmergency.state());
        }

        String operationId = "AUTO-" + UUID.randomUUID();
        LogicalTime now = logicalClock.currentTime();
        return executeAutomaticRetryLoop(
                operationId,
                OP_AUTOMATIC,
                emergencyId,
                null,
                now,
                false
        );
    }

    /**
     * Evalúa la cola FIFO {@code WAITING_AMBULANCE} para todas las ambulancias actualmente {@code AVAILABLE}
     * en orden determinista por identificador (HU-08).
     */
    public WaitingQueueEvaluationResult evaluateWaitingAmbulanceQueue() {
        List<Ambulance> availableAmbulances = listAvailableAmbulances();
        return evaluateQueueForAmbulanceList(availableAmbulances);
    }

    /**
     * Evalúa la cola FIFO {@code WAITING_AMBULANCE} cuando una ambulancia específica pasa a {@code AVAILABLE}
     * (HU-08, RN-COL-001, RN-COL-002, RN-COL-007, CA-SIM-01).
     */
    public WaitingQueueEvaluationResult evaluateWaitingAmbulanceQueueForAmbulance(String rawAmbulanceId) {
        AmbulanceId ambulanceId = parseAmbulanceId(rawAmbulanceId);
        Ambulance ambulance = ambulanceRepository.findById(ambulanceId)
                .orElseThrow(() -> new DispatchValidationException(
                        "No existe ninguna ambulancia con identificador " + ambulanceId.value() + "."));
        if (ambulance.state() != AmbulanceState.AVAILABLE || ambulance.assignedEmergencyId() != null) {
            List<Emergency> remaining = listWaitingAmbulanceQueue();
            return new WaitingQueueEvaluationResult(
                    0,
                    0,
                    remaining.size(),
                    List.of(),
                    "La ambulancia " + ambulanceId.value() + " no está AVAILABLE; no se evaluó la cola."
            );
        }
        return evaluateQueueForAmbulanceList(List.of(ambulance));
    }

    @Transactional(readOnly = true)
    public List<Emergency> listWaitingAmbulanceQueue() {
        return waitingQueuePolicy.sortFifo(
                emergencyRepository.findWaitingAmbulanceOrderByQueueEnteredAtAndId()
        );
    }

    @Transactional(readOnly = true)
    public List<Ambulance> listAvailableAmbulances() {
        return ambulanceRepository.findAllOrderById().stream()
                .filter(a -> a.state() == AmbulanceState.AVAILABLE && a.assignedEmergencyId() == null)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DispatchTraceabilityRepository.ExhaustedDispatchNotice> listRecentExhaustedNotices() {
        return traceabilityRepository.findRecentConcurrencyExhaustedNotices();
    }

    private WaitingQueueEvaluationResult evaluateQueueForAmbulanceList(List<Ambulance> availableAmbulances) {
        LogicalTime now = logicalClock.currentTime();
        List<AutomaticDispatchResult> results = new ArrayList<>();
        int assignedCount = 0;

        for (Ambulance candidateAmbulance : availableAmbulances) {
            Ambulance freshAmbulance = ambulanceRepository.findById(candidateAmbulance.id()).orElse(null);
            if (freshAmbulance == null
                    || freshAmbulance.state() != AmbulanceState.AVAILABLE
                    || freshAmbulance.assignedEmergencyId() != null) {
                continue;
            }
            List<Emergency> waitingQueue = listWaitingAmbulanceQueue();
            if (waitingQueue.isEmpty()) {
                break;
            }
            RoadGraph graph = roadGraphRepository.loadGraph();
            Optional<Emergency> selected = waitingQueuePolicy.selectNextEmergencyForAvailableAmbulance(
                    freshAmbulance,
                    waitingQueue,
                    graph
            );
            if (selected.isEmpty()) {
                continue;
            }

            // Cada operación disparada desde cola inicia su contador desde cero (HU-08 CA-3, RN-CON-009)
            String operationId = "QUEUE-" + UUID.randomUUID();
            AutomaticDispatchResult assignment = executeAutomaticRetryLoop(
                    operationId,
                    OP_WAITING_QUEUE,
                    selected.get().id(),
                    freshAmbulance.id(),
                    now,
                    true
            );
            results.add(assignment);
            if (assignment.outcome() == DispatchOperationOutcome.ASSIGNED) {
                assignedCount++;
            }
        }

        int remaining = listWaitingAmbulanceQueue().size();
        String summary;
        if (availableAmbulances.isEmpty()) {
            summary = "No existen ambulancias AVAILABLE para evaluar la cola WAITING_AMBULANCE.";
        } else if (assignedCount == 0) {
            summary = "Cola WAITING_AMBULANCE evaluada (" + availableAmbulances.size()
                    + " unidad(es) AVAILABLE): sin asignaciones compatibles/alcanzables; permanecen "
                    + remaining + " emergencia(s) en espera.";
        } else {
            summary = "Cola WAITING_AMBULANCE evaluada: " + assignedCount
                    + " asignación(es) confirmada(s) en orden FIFO; permanecen "
                    + remaining + " emergencia(s) en espera.";
        }
        return new WaitingQueueEvaluationResult(
                availableAmbulances.size(),
                assignedCount,
                remaining,
                List.copyOf(results),
                summary
        );
    }

    private AutomaticDispatchResult executeAutomaticRetryLoop(
            String operationId,
            String operationType,
            EmergencyId emergencyId,
            AmbulanceId preferredQueueAmbulanceId,
            LogicalTime now,
            boolean queueMode
    ) {
        int executedAttempts = 0;

        for (int attempt = 1; attempt <= MAX_AUTOMATIC_ATTEMPTS; attempt++) {
            Emergency freshEmergency = emergencyRepository.findById(emergencyId)
                    .orElseThrow(() -> new DispatchValidationException(
                            "No existe ninguna emergencia con identificador " + emergencyId.value() + "."));

            if (freshEmergency.state() != EmergencyState.REGISTERED
                    && freshEmergency.state() != EmergencyState.WAITING_AMBULANCE) {
                return new AutomaticDispatchResult(
                        operationId,
                        DispatchOperationOutcome.MOVED_TO_WAITING_AMBULANCE,
                        freshEmergency,
                        null,
                        executedAttempts,
                        Math.max(0, executedAttempts - 1),
                        "EMERGENCY_ALREADY_ASSIGNED",
                        "La emergencia " + emergencyId.value() + " ya pasó al estado " + freshEmergency.state() + "."
                );
            }

            List<Ambulance> catalog = ambulanceRepository.findAllOrderById();
            RoadGraph graph = roadGraphRepository.loadGraph();
            List<EligibleAmbulanceCandidate> candidates =
                    eligibilityPolicy.evaluateEligibleCandidates(freshEmergency, catalog, graph);

            AmbulanceId targetAmbulanceId = selectTargetAmbulance(candidates, preferredQueueAmbulanceId);
            if (targetAmbulanceId == null) {
                // CA-CON-04 (attempt == 1 -> 0 reintentos) y CA-CON-05 (attempt > 1 -> finalización anticipada)
                Emergency waiting = attemptExecutor.moveEmergencyToWaitingAmbulance(
                        emergencyId,
                        now,
                        "NO_ELIGIBLE_AMBULANCE"
                );
                String reason = attempt == 1
                        ? "Sin ambulancias elegibles disponibles/alcanzables en el intento inicial; la emergencia "
                                + emergencyId.value() + " queda en WAITING_AMBULANCE con 0 reintentos."
                        : "Tras conflicto concurrente ya no quedan ambulancias elegibles; finalización anticipada en WAITING_AMBULANCE.";
                traceabilityRepository.recordOperationResult(
                        operationId,
                        operationType,
                        emergencyId.value(),
                        null,
                        executedAttempts,
                        "MOVED_TO_WAITING_AMBULANCE",
                        reason,
                        now.ticks()
                );
                return new AutomaticDispatchResult(
                        operationId,
                        DispatchOperationOutcome.MOVED_TO_WAITING_AMBULANCE,
                        waiting,
                        null,
                        executedAttempts,
                        Math.max(0, executedAttempts - 1),
                        "NO_ELIGIBLE_AMBULANCE",
                        reason
                );
            }

            executedAttempts = attempt;
            SupervisedDispatchAttemptExecutor.AttemptOutcome attemptOutcome;
            try {
                attemptOutcome = queueMode
                        ? attemptExecutor.executeQueueAssignmentAttempt(emergencyId, targetAmbulanceId, now)
                        : attemptExecutor.executeAutomaticAssignmentAttempt(emergencyId, targetAmbulanceId, now);
            } catch (PessimisticLockingFailureException ex) {
                Emergency current = emergencyRepository.findById(emergencyId).orElse(freshEmergency);
                attemptOutcome = new SupervisedDispatchAttemptExecutor.AttemptOutcome(
                        false,
                        true,
                        current,
                        null,
                        "Conflicto concurrente de bloqueo en intento " + attempt + " sobre " + targetAmbulanceId.value() + "."
                );
            } catch (RuntimeException ex) {
                // Error técnico ajeno a conflicto concurrente: no consume reintentos ni marca CONCURRENCY_RETRY_EXHAUSTED (RN-CON-005)
                recordTechnicalErrorBestEffort(operationId, operationType, emergencyId.value(),
                        targetAmbulanceId.value(), attempt, ex.getMessage(), now.ticks());
                throw ex;
            }

            if (attemptOutcome.succeeded()) {
                traceabilityRepository.recordAttempt(
                        operationId,
                        operationType,
                        emergencyId.value(),
                        targetAmbulanceId.value(),
                        attempt,
                        "ASSIGNED",
                        attemptOutcome.reason(),
                        now.ticks()
                );
                traceabilityRepository.recordOperationResult(
                        operationId,
                        operationType,
                        emergencyId.value(),
                        targetAmbulanceId.value(),
                        attempt,
                        "ASSIGNED",
                        attemptOutcome.reason(),
                        now.ticks()
                );
                return new AutomaticDispatchResult(
                        operationId,
                        DispatchOperationOutcome.ASSIGNED,
                        attemptOutcome.emergency(),
                        attemptOutcome.ambulance(),
                        attempt,
                        attempt - 1,
                        "ASSIGNED",
                        attemptOutcome.reason()
                );
            }

            traceabilityRepository.recordAttempt(
                    operationId,
                    operationType,
                    emergencyId.value(),
                    targetAmbulanceId.value(),
                    attempt,
                    "AMBULANCE_UNAVAILABLE_CONFLICT",
                    attemptOutcome.reason(),
                    now.ticks()
            );
        }

        // Agotamiento de 1 intento inicial + 3 reintentos adicionales (DEC-017, RN-CON-007, CA-CON-06)
        String exhaustedMessage = "Agotamiento de reintentos por conflicto concurrente (CONCURRENCY_RETRY_EXHAUSTED) para "
                + emergencyId.value() + " tras 4 intentos (3 reintentos consumidos). La emergencia permanece en WAITING_AMBULANCE sin asignación.";
        Emergency waiting = attemptExecutor.moveEmergencyToWaitingAmbulance(
                emergencyId,
                now,
                "CONCURRENCY_RETRY_EXHAUSTED"
        );
        traceabilityRepository.recordOperationResult(
                operationId,
                operationType,
                emergencyId.value(),
                null,
                MAX_AUTOMATIC_ATTEMPTS,
                "CONCURRENCY_RETRY_EXHAUSTED",
                exhaustedMessage,
                now.ticks()
        );
        return new AutomaticDispatchResult(
                operationId,
                DispatchOperationOutcome.CONCURRENCY_RETRY_EXHAUSTED,
                waiting,
                null,
                MAX_AUTOMATIC_ATTEMPTS,
                MAX_AUTOMATIC_ATTEMPTS - 1,
                "CONCURRENCY_RETRY_EXHAUSTED",
                exhaustedMessage
        );
    }

    private static AmbulanceId selectTargetAmbulance(
            List<EligibleAmbulanceCandidate> candidates,
            AmbulanceId preferredQueueAmbulanceId
    ) {
        if (candidates.isEmpty()) {
            return null;
        }
        if (preferredQueueAmbulanceId != null) {
            boolean preferredIsEligible = candidates.stream()
                    .anyMatch(c -> c.ambulance().id().equals(preferredQueueAmbulanceId));
            if (preferredIsEligible) {
                return preferredQueueAmbulanceId;
            }
        }
        return candidates.get(0).ambulance().id();
    }

    private void recordTechnicalErrorBestEffort(
            String operationId,
            String operationType,
            String emergencyId,
            String candidateResourceId,
            int attemptNumber,
            String detailMessage,
            long logicalTicks
    ) {
        String safeMessage = detailMessage == null || detailMessage.isBlank()
                ? "Error técnico no reintentable durante la asignación automática."
                : detailMessage;
        try {
            traceabilityRepository.recordAttempt(
                    operationId,
                    operationType,
                    emergencyId,
                    candidateResourceId,
                    attemptNumber,
                    "TECHNICAL_ERROR",
                    safeMessage,
                    logicalTicks
            );
            traceabilityRepository.recordOperationResult(
                    operationId,
                    operationType,
                    emergencyId,
                    null,
                    attemptNumber,
                    "TECHNICAL_ERROR",
                    safeMessage,
                    logicalTicks
            );
        } catch (RuntimeException ignored) {
            // No ocultar el error técnico original si falla el registro de trazabilidad
        }
    }

    private static EmergencyId parseEmergencyId(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DispatchValidationException("El identificador de la emergencia es obligatorio.");
        }
        try {
            return EmergencyId.of(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new DispatchValidationException(ex.getMessage());
        }
    }

    private static AmbulanceId parseAmbulanceId(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DispatchValidationException("Debe seleccionar una ambulancia elegible para asignar.");
        }
        try {
            return AmbulanceId.of(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new DispatchValidationException(ex.getMessage());
        }
    }
}

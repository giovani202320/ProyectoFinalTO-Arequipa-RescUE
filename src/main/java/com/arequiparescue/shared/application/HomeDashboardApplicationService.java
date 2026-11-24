package com.arequiparescue.shared.application;

import com.arequiparescue.ambulance.application.AmbulanceCatalogApplicationService;
import com.arequiparescue.ambulance.domain.Ambulance;
import com.arequiparescue.ambulance.domain.AmbulanceState;
import com.arequiparescue.emergency.application.EmergencyApplicationService;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyState;
import com.arequiparescue.hospital.application.HospitalCatalogApplicationService;
import com.arequiparescue.hospital.domain.Hospital;
import com.arequiparescue.shared.domain.LogicalClock;
import com.arequiparescue.simulation.application.SimulationScenarioApplicationService;
import com.arequiparescue.simulation.domain.SimulationScenario;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio de aplicación de solo lectura que consolida el estado operativo actual del simulador
 * para la página de inicio ({@code GET /}, P1-3), reutilizando exclusivamente los servicios de
 * aplicación existentes sin consultas N+1 ni acceso directo a JPA.
 */
@Service
public class HomeDashboardApplicationService {

    private final EmergencyApplicationService emergencyService;
    private final AmbulanceCatalogApplicationService ambulanceService;
    private final HospitalCatalogApplicationService hospitalService;
    private final SimulationScenarioApplicationService scenarioService;
    private final LogicalClock logicalClock;
    private final Environment environment;

    public HomeDashboardApplicationService(
            EmergencyApplicationService emergencyService,
            AmbulanceCatalogApplicationService ambulanceService,
            HospitalCatalogApplicationService hospitalService,
            SimulationScenarioApplicationService scenarioService,
            LogicalClock logicalClock,
            Environment environment
    ) {
        this.emergencyService = Objects.requireNonNull(emergencyService);
        this.ambulanceService = Objects.requireNonNull(ambulanceService);
        this.hospitalService = Objects.requireNonNull(hospitalService);
        this.scenarioService = Objects.requireNonNull(scenarioService);
        this.logicalClock = Objects.requireNonNull(logicalClock);
        this.environment = Objects.requireNonNull(environment);
    }

    @Transactional(readOnly = true)
    public HomeDashboardSnapshot loadDashboardSnapshot() {
        List<EmergencyState> emergencyStates = emergencyService.listEmergencies().stream()
                .map(Emergency::state)
                .toList();
        List<AmbulanceState> ambulanceStates = ambulanceService.listAmbulances().stream()
                .map(Ambulance::state)
                .toList();
        List<HospitalCapacityRecord> hospitals = hospitalService.listHospitals().stream()
                .map(HomeDashboardApplicationService::toHospitalCapacityRecord)
                .toList();
        Optional<HomeDashboardSnapshot.ActiveScenarioInfo> activeScenario = scenarioService.findActiveScenario()
                .map(HomeDashboardApplicationService::toActiveScenarioInfo);
        long globalTicks = Math.max(0L, logicalClock.currentTicks());
        String[] activeProfiles = environment.getActiveProfiles();

        return buildSnapshot(
                emergencyStates,
                ambulanceStates,
                hospitals,
                activeScenario.orElse(null),
                globalTicks,
                activeProfiles
        );
    }

    /**
     * Entrada inmutable de capacidad hospitalaria para permitir verificación determinista de
     * consistencia (incluyendo detección de registros inconsistentes o negativos) sin alterar el dominio.
     */
    public record HospitalCapacityRecord(
            String hospitalId,
            boolean enabled,
            int totalCapacity,
            int reservedSlots,
            int occupiedSlots
    ) {
    }

    public static HomeDashboardSnapshot buildSnapshot(
            List<EmergencyState> emergencyStates,
            List<AmbulanceState> ambulanceStates,
            List<HospitalCapacityRecord> hospitals,
            HomeDashboardSnapshot.ActiveScenarioInfo activeScenario,
            long globalLogicalTicks,
            String[] activeProfiles
    ) {
        List<EmergencyState> safeEmergencies = emergencyStates != null ? emergencyStates : List.of();
        List<AmbulanceState> safeAmbulances = ambulanceStates != null ? ambulanceStates : List.of();
        List<HospitalCapacityRecord> safeHospitals = hospitals != null ? hospitals : List.of();

        // 1. Emergencias y distribución por los 7 estados
        Map<EmergencyState, Long> countsByState = new EnumMap<>(EmergencyState.class);
        for (EmergencyState state : EmergencyState.values()) {
            countsByState.put(state, 0L);
        }
        long activeEmergencies = 0L;
        long completedEmergencies = 0L;
        for (EmergencyState state : safeEmergencies) {
            if (state == null) {
                continue;
            }
            countsByState.merge(state, 1L, Long::sum);
            if (state == EmergencyState.COMPLETED) {
                completedEmergencies++;
            } else {
                activeEmergencies++;
            }
        }
        long totalEmergencies = activeEmergencies + completedEmergencies;
        long waitingAmbulanceCount = countsByState.getOrDefault(EmergencyState.WAITING_AMBULANCE, 0L);
        long waitingHospitalCount = countsByState.getOrDefault(EmergencyState.WAITING_HOSPITAL, 0L);

        List<HomeDashboardSnapshot.EmergencyStateCount> distribution = new ArrayList<>();
        long stateSum = 0L;
        for (EmergencyState state : EmergencyState.values()) {
            long count = countsByState.getOrDefault(state, 0L);
            stateSum += count;
            double sharePercent = totalEmergencies > 0L
                    ? Math.round((count * 1000.0) / totalEmergencies) / 10.0
                    : 0.0;
            distribution.add(new HomeDashboardSnapshot.EmergencyStateCount(state.name(), count, sharePercent));
        }
        boolean stateDistributionConsistent = (stateSum == totalEmergencies);

        // 2. Ambulancias (disponibles en catálogo vs. ocupadas; no implica elegibilidad automática)
        long availableAmbulances = 0L;
        long occupiedAmbulances = 0L;
        for (AmbulanceState state : safeAmbulances) {
            if (state == null) {
                continue;
            }
            if (state == AmbulanceState.AVAILABLE) {
                availableAmbulances++;
            } else {
                occupiedAmbulances++;
            }
        }
        long totalAmbulances = availableAmbulances + occupiedAmbulances;

        // 3. Hospitales y capacidad (C, R, O, D sin truncamiento arbitrario)
        int enabledHospitals = 0;
        int disabledHospitals = 0;
        int enabledCapacityTotal = 0;
        int enabledReservedSlots = 0;
        int enabledOccupiedSlots = 0;
        int enabledAvailableCapacity = 0;
        int inconsistentHospitalsCount = 0;

        for (HospitalCapacityRecord h : safeHospitals) {
            int available = h.totalCapacity() - h.reservedSlots() - h.occupiedSlots();
            boolean valid = h.totalCapacity() >= 0
                    && h.reservedSlots() >= 0
                    && h.occupiedSlots() >= 0
                    && available >= 0
                    && (h.reservedSlots() + h.occupiedSlots() + available == h.totalCapacity());
            if (!valid) {
                inconsistentHospitalsCount++;
            }
            if (h.enabled()) {
                enabledHospitals++;
                enabledCapacityTotal += h.totalCapacity();
                enabledReservedSlots += h.reservedSlots();
                enabledOccupiedSlots += h.occupiedSlots();
                enabledAvailableCapacity += available;
            } else {
                disabledHospitals++;
            }
        }
        int totalHospitals = enabledHospitals + disabledHospitals;
        boolean hospitalCapacityConsistent = (inconsistentHospitalsCount == 0);

        boolean emptyDatabase = (totalEmergencies == 0L
                && totalAmbulances == 0L
                && totalHospitals == 0
                && activeScenario == null);

        HomeDashboardSnapshot.EnvironmentProfileInfo profileInfo = resolveEnvironmentProfile(activeProfiles);

        // 4. Avisos operativos condicionales respaldados exclusivamente por datos reales
        List<HomeDashboardSnapshot.OperationalNotice> notices = new ArrayList<>();
        if (!hospitalCapacityConsistent) {
            notices.add(new HomeDashboardSnapshot.OperationalNotice(
                    "danger",
                    "Se detectaron " + inconsistentHospitalsCount
                            + " hospital(es) con valores de capacidad inconsistentes (C < R + O o negativos).",
                    "Revisar hospitales",
                    "/admin/hospitals"
            ));
        }
        if (waitingAmbulanceCount > 0L) {
            notices.add(new HomeDashboardSnapshot.OperationalNotice(
                    "warning",
                    "Hay " + waitingAmbulanceCount + " emergencia(s) en cola esperando asignación de ambulancia.",
                    "Ir a cola de ambulancias",
                    "/dispatch/waiting-queue"
            ));
        }
        if (waitingHospitalCount > 0L) {
            notices.add(new HomeDashboardSnapshot.OperationalNotice(
                    "warning",
                    "Hay " + waitingHospitalCount + " emergencia(s) en espera de cupo hospitalario.",
                    "Ir a cola hospitalaria",
                    "/dispatch/hospital-queue"
            ));
        }
        if (totalAmbulances > 0L && availableAmbulances == 0L) {
            notices.add(new HomeDashboardSnapshot.OperationalNotice(
                    "warning",
                    "Todas las ambulancias registradas (" + totalAmbulances + ") se encuentran actualmente ocupadas.",
                    "Ver flota",
                    "/admin/ambulances"
            ));
        }
        if (enabledHospitals > 0 && enabledAvailableCapacity <= 0) {
            notices.add(new HomeDashboardSnapshot.OperationalNotice(
                    "warning",
                    "La capacidad disponible agregada en hospitales habilitados es " + enabledAvailableCapacity
                            + " sobre C=" + enabledCapacityTotal + ".",
                    "Ver hospitales",
                    "/admin/hospitals"
            ));
        }
        if (activeScenario != null) {
            notices.add(new HomeDashboardSnapshot.OperationalNotice(
                    "info",
                    "Escenario activo " + activeScenario.scenarioId()
                            + ": la edición de catálogos de ambulancias y hospitales permanece restringida mientras el escenario continúe ACTIVE.",
                    "Ir a simulación",
                    "/simulation"
            ));
        }

        return new HomeDashboardSnapshot(
                profileInfo,
                Math.max(0L, globalLogicalTicks),
                activeScenario,
                totalEmergencies,
                activeEmergencies,
                completedEmergencies,
                waitingAmbulanceCount,
                waitingHospitalCount,
                List.copyOf(distribution),
                stateDistributionConsistent,
                totalAmbulances,
                availableAmbulances,
                occupiedAmbulances,
                totalHospitals,
                enabledHospitals,
                disabledHospitals,
                enabledCapacityTotal,
                enabledReservedSlots,
                enabledOccupiedSlots,
                enabledAvailableCapacity,
                hospitalCapacityConsistent,
                inconsistentHospitalsCount,
                emptyDatabase,
                List.copyOf(notices)
        );
    }

    static HomeDashboardSnapshot.EnvironmentProfileInfo resolveEnvironmentProfile(String[] activeProfiles) {
        List<String> normalized = activeProfiles == null
                ? List.of()
                : Arrays.stream(activeProfiles)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (normalized.isEmpty()) {
            return new HomeDashboardSnapshot.EnvironmentProfileInfo(
                    HomeDashboardSnapshot.EnvironmentProfileKind.UNIDENTIFIED,
                    "Perfil activo: no identificado (default)",
                    "Sin perfil Spring explícito; la fuente de datos depende de la configuración del entorno."
            );
        }

        List<String> lower = normalized.stream()
                .map(p -> p.toLowerCase(Locale.ROOT))
                .toList();

        if (lower.contains("demo")) {
            return new HomeDashboardSnapshot.EnvironmentProfileInfo(
                    HomeDashboardSnapshot.EnvironmentProfileKind.DEMO,
                    "Perfil activo: demo",
                    "Perfil de demostración académica."
            );
        }
        if (lower.contains("historical")) {
            return new HomeDashboardSnapshot.EnvironmentProfileInfo(
                    HomeDashboardSnapshot.EnvironmentProfileKind.HISTORICAL,
                    "Perfil activo: historical",
                    "Perfil histórico configurado explícitamente."
            );
        }
        if (lower.contains("mysql-test")) {
            return new HomeDashboardSnapshot.EnvironmentProfileInfo(
                    HomeDashboardSnapshot.EnvironmentProfileKind.MYSQL_TEST,
                    "Perfil activo: mysql-test",
                    "Perfil de pruebas de integración."
            );
        }

        String safeNames = normalized.stream()
                .map(HomeDashboardApplicationService::sanitizeProfileToken)
                .collect(Collectors.joining(", "));
        return new HomeDashboardSnapshot.EnvironmentProfileInfo(
                HomeDashboardSnapshot.EnvironmentProfileKind.CUSTOM,
                "Perfil activo: personalizado (" + safeNames + ")",
                "Perfil personalizado de Spring sin credenciales ni parámetros de conexión expuestos."
        );
    }

    private static String sanitizeProfileToken(String profile) {
        String cleaned = profile.replaceAll("[^A-Za-z0-9._-]", "");
        return cleaned.isEmpty() ? "custom" : cleaned;
    }

    private static HospitalCapacityRecord toHospitalCapacityRecord(Hospital h) {
        return new HospitalCapacityRecord(
                h.id().value(),
                h.enabled(),
                h.totalCapacity(),
                h.reservedSlots(),
                h.occupiedSlots()
        );
    }

    private static HomeDashboardSnapshot.ActiveScenarioInfo toActiveScenarioInfo(SimulationScenario s) {
        return new HomeDashboardSnapshot.ActiveScenarioInfo(
                s.id(),
                s.name(),
                s.currentTime().ticks()
        );
    }
}

package com.arequiparescue.dashboard.application;

import com.arequiparescue.ambulance.domain.AmbulanceState;
import com.arequiparescue.emergency.domain.EmergencyState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeDashboardApplicationServiceTest {

    @Test
    void handlesEmptyDatabaseWithoutDivisionByZeroOrFalseNotices() {
        HomeDashboardSnapshot snapshot = HomeDashboardApplicationService.buildSnapshot(
                List.of(),
                List.of(),
                List.of(),
                null,
                0L,
                new String[]{"demo"}
        );

        assertTrue(snapshot.emptyDatabase());
        assertEquals(0L, snapshot.totalEmergencies());
        assertEquals(0L, snapshot.activeEmergencies());
        assertEquals(0L, snapshot.completedEmergencies());
        assertEquals(7, snapshot.stateDistribution().size());
        assertTrue(snapshot.stateDistributionConsistent());
        assertTrue(snapshot.stateDistribution().stream().allMatch(s -> s.count() == 0L && s.sharePercent() == 0.0));
        assertEquals(0L, snapshot.totalAmbulances());
        assertEquals(0, snapshot.totalHospitals());
        assertTrue(snapshot.hospitalCapacityConsistent());
        assertNull(snapshot.activeScenario());
        assertTrue(snapshot.notices().isEmpty());
        assertEquals(HomeDashboardSnapshot.EnvironmentProfileKind.DEMO, snapshot.environmentProfile().kind());
        assertEquals("Perfil activo: demo", snapshot.environmentProfile().displayLabel());
    }

    @Test
    void computesConsistentIndicatorsDifferentiatingClocksEnabledHospitalsAndRealNotices() {
        List<EmergencyState> emergencies = List.of(
                EmergencyState.REGISTERED,
                EmergencyState.WAITING_AMBULANCE,
                EmergencyState.ASSIGNED,
                EmergencyState.AT_INCIDENT,
                EmergencyState.WAITING_HOSPITAL,
                EmergencyState.IN_TRANSPORT,
                EmergencyState.COMPLETED,
                EmergencyState.COMPLETED
        );
        List<AmbulanceState> ambulances = List.of(
                AmbulanceState.AVAILABLE,
                AmbulanceState.ASSIGNED,
                AmbulanceState.TRANSPORTING
        );
        List<HomeDashboardApplicationService.HospitalCapacityRecord> hospitals = List.of(
                new HomeDashboardApplicationService.HospitalCapacityRecord("HOSP-01", true, 5, 1, 2),
                new HomeDashboardApplicationService.HospitalCapacityRecord("HOSP-02", true, 3, 0, 1),
                new HomeDashboardApplicationService.HospitalCapacityRecord("HOSP-03", false, 4, 0, 0)
        );
        HomeDashboardSnapshot.ActiveScenarioInfo activeScenario =
                new HomeDashboardSnapshot.ActiveScenarioInfo("SCN-09", "Escenario Demo", 12L);

        HomeDashboardSnapshot snapshot = HomeDashboardApplicationService.buildSnapshot(
                emergencies,
                ambulances,
                hospitals,
                activeScenario,
                25L,
                new String[]{"historical"}
        );

        assertFalse(snapshot.emptyDatabase());
        // Emergencias: total = activas + completadas, suma de los 7 estados == total
        assertEquals(8L, snapshot.totalEmergencies());
        assertEquals(6L, snapshot.activeEmergencies());
        assertEquals(2L, snapshot.completedEmergencies());
        assertEquals(snapshot.totalEmergencies(), snapshot.activeEmergencies() + snapshot.completedEmergencies());
        assertEquals(1L, snapshot.waitingAmbulanceCount());
        assertEquals(1L, snapshot.waitingHospitalCount());
        assertTrue(snapshot.stateDistributionConsistent());
        assertEquals(8L, snapshot.stateDistribution().stream().mapToLong(HomeDashboardSnapshot.EmergencyStateCount::count).sum());

        // Ambulancias: total = disponibles + ocupadas
        assertEquals(3L, snapshot.totalAmbulances());
        assertEquals(1L, snapshot.availableAmbulances());
        assertEquals(2L, snapshot.occupiedAmbulances());
        assertEquals(snapshot.totalAmbulances(), snapshot.availableAmbulances() + snapshot.occupiedAmbulances());

        // Hospitales habilitados solamente (HOSP-01 + HOSP-02; HOSP-03 deshabilitado excluido)
        assertEquals(3, snapshot.totalHospitals());
        assertEquals(2, snapshot.enabledHospitals());
        assertEquals(1, snapshot.disabledHospitals());
        assertEquals(8, snapshot.enabledCapacityTotal());
        assertEquals(1, snapshot.enabledReservedSlots());
        assertEquals(3, snapshot.enabledOccupiedSlots());
        assertEquals(4, snapshot.enabledAvailableCapacity());
        assertTrue(snapshot.hospitalCapacityConsistent());

        // Relojes diferenciados
        assertEquals(25L, snapshot.globalLogicalTicks());
        assertEquals("SCN-09", snapshot.activeScenario().scenarioId());
        assertEquals(12L, snapshot.activeScenario().scenarioLogicalTicks());

        // Avisos reales: waiting ambulance, waiting hospital, active scenario (NO flota saturada ni capacidad 0)
        assertEquals(3, snapshot.notices().size());
        assertTrue(snapshot.notices().stream().anyMatch(n -> n.actionPath().equals("/dispatch/waiting-queue")));
        assertTrue(snapshot.notices().stream().anyMatch(n -> n.actionPath().equals("/dispatch/hospital-queue")));
        assertTrue(snapshot.notices().stream().anyMatch(n -> n.actionPath().equals("/simulation")));
        assertFalse(snapshot.notices().stream().anyMatch(n -> n.actionPath().equals("/admin/ambulances")));
        assertFalse(snapshot.notices().stream().anyMatch(n -> n.actionPath().equals("/admin/hospitals")));
    }

    @Test
    void surfacesInconsistentHospitalCapacityWithoutArbitraryTruncationAndEmitsNotices() {
        List<AmbulanceState> saturatedFleet = List.of(AmbulanceState.ASSIGNED, AmbulanceState.TRANSPORTING);
        List<HomeDashboardApplicationService.HospitalCapacityRecord> hospitals = List.of(
                // Inconsistente: C=2, R=2, O=2 -> D = -2 (no truncar a 0)
                new HomeDashboardApplicationService.HospitalCapacityRecord("HOSP-BAD", true, 2, 2, 2)
        );

        HomeDashboardSnapshot snapshot = HomeDashboardApplicationService.buildSnapshot(
                List.of(EmergencyState.COMPLETED),
                saturatedFleet,
                hospitals,
                null,
                5L,
                new String[]{"mysql-test"}
        );

        assertFalse(snapshot.hospitalCapacityConsistent());
        assertEquals(1, snapshot.inconsistentHospitalsCount());
        assertEquals(-2, snapshot.enabledAvailableCapacity());
        assertTrue(snapshot.notices().stream().anyMatch(n -> n.severity().equals("danger") && n.message().contains("inconsistentes")));
        assertTrue(snapshot.notices().stream().anyMatch(n -> n.actionPath().equals("/admin/ambulances")));
        assertTrue(snapshot.notices().stream().anyMatch(n -> n.message().contains("capacidad disponible agregada")));
    }

    @Test
    void identifiesAllProfileCategoriesExplicitlyWithoutExposingCredentials() {
        assertEquals(HomeDashboardSnapshot.EnvironmentProfileKind.DEMO,
                HomeDashboardApplicationService.resolveEnvironmentProfile(new String[]{"demo"}).kind());
        assertEquals(HomeDashboardSnapshot.EnvironmentProfileKind.HISTORICAL,
                HomeDashboardApplicationService.resolveEnvironmentProfile(new String[]{"historical"}).kind());
        assertEquals(HomeDashboardSnapshot.EnvironmentProfileKind.MYSQL_TEST,
                HomeDashboardApplicationService.resolveEnvironmentProfile(new String[]{"mysql-test"}).kind());

        HomeDashboardSnapshot.EnvironmentProfileInfo custom =
                HomeDashboardApplicationService.resolveEnvironmentProfile(new String[]{"staging-lab"});
        assertEquals(HomeDashboardSnapshot.EnvironmentProfileKind.CUSTOM, custom.kind());
        assertTrue(custom.displayLabel().contains("staging-lab"));

        HomeDashboardSnapshot.EnvironmentProfileInfo unidentified =
                HomeDashboardApplicationService.resolveEnvironmentProfile(new String[0]);
        assertEquals(HomeDashboardSnapshot.EnvironmentProfileKind.UNIDENTIFIED, unidentified.kind());
        assertTrue(unidentified.displayLabel().contains("no identificado"));
        assertFalse(unidentified.displayLabel().toLowerCase().contains("historical"));
    }
}

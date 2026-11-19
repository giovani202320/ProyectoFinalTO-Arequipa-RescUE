package com.arequiparescue;

import com.arequiparescue.ambulance.application.AmbulanceCatalogApplicationService;
import com.arequiparescue.ambulance.application.RegisterAmbulanceCommand;
import com.arequiparescue.ambulance.infrastructure.persistence.SpringDataAmbulanceRepository;
import com.arequiparescue.dispatch.application.DispatchApplicationService;
import com.arequiparescue.dispatch.application.SupervisedDispatchResult;
import com.arequiparescue.dispatch.domain.DispatchOperationOutcome;
import com.arequiparescue.dispatch.infrastructure.persistence.SpringDataExperimentOperationResultRepository;
import com.arequiparescue.dispatch.infrastructure.persistence.SpringDataOperationAttemptLogRepository;
import com.arequiparescue.emergency.application.EmergencyApplicationService;
import com.arequiparescue.emergency.application.RegisterEmergencyCommand;
import com.arequiparescue.emergency.domain.Emergency;
import com.arequiparescue.emergency.domain.EmergencyRouteSegment;
import com.arequiparescue.emergency.domain.EmergencyState;
import com.arequiparescue.emergency.domain.RouteSegmentType;
import com.arequiparescue.emergency.domain.port.EmergencyRepository;
import com.arequiparescue.emergency.domain.port.EmergencyRouteSegmentRepository;
import com.arequiparescue.emergency.infrastructure.persistence.EmergencySequenceJpaEntity;
import com.arequiparescue.emergency.infrastructure.persistence.SpringDataEmergencyRepository;
import com.arequiparescue.emergency.infrastructure.persistence.SpringDataEmergencySequenceRepository;
import com.arequiparescue.hospital.application.HospitalCatalogApplicationService;
import com.arequiparescue.hospital.application.HospitalDeliveryAndDischargeApplicationService;
import com.arequiparescue.hospital.application.HospitalDispatchApplicationService;
import com.arequiparescue.hospital.application.HospitalReservationResult;
import com.arequiparescue.hospital.application.RegisterHospitalCommand;
import com.arequiparescue.hospital.domain.HospitalDispatchOutcome;
import com.arequiparescue.hospital.infrastructure.persistence.SpringDataHospitalRepository;
import com.arequiparescue.hospital.infrastructure.persistence.SpringDataHospitalReservationRepository;
import com.arequiparescue.routing.domain.DijkstraPathResult;
import com.arequiparescue.routing.domain.DijkstraRouteCalculator;
import com.arequiparescue.routing.domain.NodeId;
import com.arequiparescue.routing.domain.RoadGraph;
import com.arequiparescue.routing.domain.port.RoadGraphRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("mysql-test")
@Tag("mysql-integration")
@EnabledIfSystemProperty(named = "are.mysql.integration", matches = "true")
@DisplayName("Integración MySQL 8.4 + ADR-0003 Persistencia Histórica de Tramos Dijkstra")
class EmergencyRouteSegmentMySqlIntegrationTest {

    @Autowired
    private EmergencyApplicationService emergencyApplicationService;

    @Autowired
    private AmbulanceCatalogApplicationService ambulanceCatalogService;

    @Autowired
    private HospitalCatalogApplicationService hospitalCatalogService;

    @Autowired
    private DispatchApplicationService dispatchApplicationService;

    @Autowired
    private HospitalDispatchApplicationService hospitalDispatchApplicationService;

    @Autowired
    private HospitalDeliveryAndDischargeApplicationService deliveryAndDischargeApplicationService;

    @Autowired
    private EmergencyRouteSegmentRepository routeSegmentRepository;

    @Autowired
    private EmergencyRepository emergencyRepository;

    @Autowired
    private RoadGraphRepository roadGraphRepository;

    @Autowired
    private SpringDataHospitalReservationRepository springDataHospitalReservationRepository;

    @Autowired
    private SpringDataOperationAttemptLogRepository attemptLogRepository;

    @Autowired
    private SpringDataExperimentOperationResultRepository operationResultRepository;

    @Autowired
    private SpringDataAmbulanceRepository springDataAmbulanceRepository;

    @Autowired
    private SpringDataEmergencyRepository springDataEmergencyRepository;

    @Autowired
    private SpringDataHospitalRepository springDataHospitalRepository;

    @Autowired
    private SpringDataEmergencySequenceRepository springDataEmergencySequenceRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void resetDatabaseState() {
        MySqlTestSchemaGuard.assertIsolatedTestSchema(jdbcTemplate);
        jdbcTemplate.update("DELETE FROM emergency_route_segments");
        springDataHospitalReservationRepository.deleteAll();
        attemptLogRepository.deleteAll();
        operationResultRepository.deleteAll();
        springDataAmbulanceRepository.deleteAll();
        springDataEmergencyRepository.deleteAll();
        springDataHospitalRepository.deleteAll();

        springDataEmergencySequenceRepository.saveAndFlush(new EmergencySequenceJpaEntity("EMERGENCY", 1L));
        springDataEmergencySequenceRepository.saveAndFlush(new EmergencySequenceJpaEntity("AMBULANCE", 1L));
        springDataEmergencySequenceRepository.saveAndFlush(new EmergencySequenceJpaEntity("HOSPITAL", 1L));
    }

    @Test
    @DisplayName("Flyway V8 aplicada y ciclo completo persiste Tramo A y Tramo B históricos incluso tras COMPLETED y DISCHARGED")
    void shouldPersistBothHistoricalSegmentsAcrossFullLifecycle() {
        Integer v8Count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '8' AND success = 1",
                Integer.class);
        assertThat(v8Count).isEqualTo(1);

        ambulanceCatalogService.registerAmbulance(new RegisterAmbulanceCommand("AMB-01", "BASIC", "N01"));
        hospitalCatalogService.registerHospital(new RegisterHospitalCommand("HOSP-01", "Hospital Central", "N04", true, 1));
        Emergency emergency = emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-B", "N03", "Paciente estable en N03"));

        RoadGraph graph = roadGraphRepository.loadGraph();
        DijkstraRouteCalculator calculator = new DijkstraRouteCalculator();
        DijkstraPathResult expectedA = calculator.shortestPath(graph, NodeId.of("N01"), NodeId.of("N03")).orElseThrow();
        DijkstraPathResult expectedB = calculator.shortestPath(graph, NodeId.of("N03"), NodeId.of("N04")).orElseThrow();

        SupervisedDispatchResult dispatchResult =
                dispatchApplicationService.assignSupervised(emergency.id().value(), "AMB-01");
        assertThat(dispatchResult.outcome()).isEqualTo(DispatchOperationOutcome.ASSIGNED);

        EmergencyRouteSegment persistedA = routeSegmentRepository
                .findByEmergencyIdAndType(emergency.id(), RouteSegmentType.AMBULANCE_TO_INCIDENT)
                .orElseThrow();
        assertThat(persistedA.resourceId()).isEqualTo("AMB-01");
        assertThat(persistedA.originNodeId()).isEqualTo(NodeId.of("N01"));
        assertThat(persistedA.destinationNodeId()).isEqualTo(NodeId.of("N03"));
        assertThat(persistedA.pathNodes()).containsExactlyElementsOf(expectedA.path());
        assertThat(persistedA.abstractCost()).isEqualTo(expectedA.totalCost());

        HospitalReservationResult hospitalResult =
                hospitalDispatchApplicationService.confirmArrivalAndReserveHospital(emergency.id().value());
        assertThat(hospitalResult.outcome()).isEqualTo(HospitalDispatchOutcome.IN_TRANSPORT);

        var delivery = deliveryAndDischargeApplicationService.confirmHospitalDelivery(emergency.id().value());
        deliveryAndDischargeApplicationService.executeSimulatedDischarge(delivery.reservation().id());

        List<EmergencyRouteSegment> segmentsAfterDischarge =
                emergencyApplicationService.findRouteSegments(emergency.id());
        assertThat(segmentsAfterDischarge).hasSize(2);

        EmergencyRouteSegment finalA = segmentsAfterDischarge.stream()
                .filter(s -> s.segmentType() == RouteSegmentType.AMBULANCE_TO_INCIDENT)
                .findFirst()
                .orElseThrow();
        EmergencyRouteSegment finalB = segmentsAfterDischarge.stream()
                .filter(s -> s.segmentType() == RouteSegmentType.INCIDENT_TO_HOSPITAL)
                .findFirst()
                .orElseThrow();

        assertThat(finalA.originNodeId()).isEqualTo(NodeId.of("N01"));
        assertThat(finalA.destinationNodeId()).isEqualTo(NodeId.of("N03"));
        assertThat(finalA.pathNodes()).containsExactlyElementsOf(expectedA.path());
        assertThat(finalA.abstractCost()).isEqualTo(expectedA.totalCost());

        assertThat(finalB.resourceId()).isEqualTo("HOSP-01");
        assertThat(finalB.originNodeId()).isEqualTo(NodeId.of("N03"));
        assertThat(finalB.destinationNodeId()).isEqualTo(NodeId.of("N04"));
        assertThat(finalB.pathNodes()).containsExactlyElementsOf(expectedB.path());
        assertThat(finalB.abstractCost()).isEqualTo(expectedB.totalCost());

        // Idempotencia de saveIfAbsent: no sobreescribe ni falla ante reintento
        EmergencyRouteSegment duplicateAttempt = new EmergencyRouteSegment(
                emergency.id(),
                RouteSegmentType.INCIDENT_TO_HOSPITAL,
                "HOSP-99",
                NodeId.of("N03"),
                NodeId.of("N04"),
                expectedB.path(),
                999,
                99L);
        routeSegmentRepository.saveIfAbsent(duplicateAttempt);
        EmergencyRouteSegment unchangedB = routeSegmentRepository
                .findByEmergencyIdAndType(emergency.id(), RouteSegmentType.INCIDENT_TO_HOSPITAL)
                .orElseThrow();
        assertThat(unchangedB.resourceId()).isEqualTo("HOSP-01");
        assertThat(unchangedB.abstractCost()).isEqualTo(expectedB.totalCost());
    }

    @Test
    @DisplayName("Concurrencia en despacho: solo el ganador persiste Tramo A; el perdedor no persiste segmento ni duplica")
    void shouldPersistSegmentOnlyForWinningConcurrentDispatch() throws Exception {
        ambulanceCatalogService.registerAmbulance(new RegisterAmbulanceCommand("AMB-01", "BASIC", "N01"));
        Emergency e1 = emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-B", "N02", "Caso 1"));
        Emergency e2 = emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-B", "N03", "Caso 2"));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<SupervisedDispatchResult> task1 = () -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            return dispatchApplicationService.assignSupervised(e1.id().value(), "AMB-01");
        };
        Callable<SupervisedDispatchResult> task2 = () -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            return dispatchApplicationService.assignSupervised(e2.id().value(), "AMB-01");
        };

        Future<SupervisedDispatchResult> f1 = pool.submit(task1);
        Future<SupervisedDispatchResult> f2 = pool.submit(task2);
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        SupervisedDispatchResult r1 = f1.get(15, TimeUnit.SECONDS);
        SupervisedDispatchResult r2 = f2.get(15, TimeUnit.SECONDS);
        pool.shutdown();

        long assignedCount = List.of(r1, r2).stream()
                .filter(r -> r.outcome() == DispatchOperationOutcome.ASSIGNED)
                .count();
        assertThat(assignedCount).isEqualTo(1);

        Emergency winner = r1.outcome() == DispatchOperationOutcome.ASSIGNED ? e1 : e2;
        Emergency loser = r1.outcome() == DispatchOperationOutcome.ASSIGNED ? e2 : e1;

        assertThat(routeSegmentRepository.findByEmergencyId(winner.id())).hasSize(1);
        assertThat(routeSegmentRepository.findByEmergencyId(loser.id())).isEmpty();
    }

    @Test
    @DisplayName("Colas WAITING_AMBULANCE y WAITING_HOSPITAL persisten Tramo A y Tramo B al reevaluarse")
    void shouldPersistSegmentsWhenQueuedEmergenciesAreReevaluated() {
        ambulanceCatalogService.registerAmbulance(new RegisterAmbulanceCommand("AMB-01", "BASIC", "N01"));
        hospitalCatalogService.registerHospital(new RegisterHospitalCommand("HOSP-01", "Hospital Base", "N04", true, 1));

        Emergency first = emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-B", "N02", "Primera emergencia"));
        Emergency second = emergencyApplicationService.registerEmergency(
                new RegisterEmergencyCommand("INC-B", "N03", "Segunda en espera"));

        assertThat(dispatchApplicationService.assignSupervised(first.id().value(), "AMB-01").outcome())
                .isEqualTo(DispatchOperationOutcome.ASSIGNED);
        // No hay ambulancia libre: la segunda pasa a WAITING_AMBULANCE sin Tramo A
        dispatchApplicationService.assignAutomatic(second.id().value());
        assertThat(emergencyRepository.findById(second.id()).orElseThrow().state())
                .isEqualTo(EmergencyState.WAITING_AMBULANCE);
        assertThat(routeSegmentRepository.findByEmergencyId(second.id())).isEmpty();

        // Primera llega, reserva HOSP-01 y completa entrega -> AMB-01 queda libre en N04 y reevalúa segunda!
        assertThat(hospitalDispatchApplicationService.confirmArrivalAndReserveHospital(first.id().value()).outcome())
                .isEqualTo(HospitalDispatchOutcome.IN_TRANSPORT);
        var firstDelivery = deliveryAndDischargeApplicationService.confirmHospitalDelivery(first.id().value());

        Emergency secondAfterDelivery = emergencyRepository.findById(second.id()).orElseThrow();
        assertThat(secondAfterDelivery.state()).isEqualTo(EmergencyState.ASSIGNED);

        EmergencyRouteSegment secondA = routeSegmentRepository
                .findByEmergencyIdAndType(second.id(), RouteSegmentType.AMBULANCE_TO_INCIDENT)
                .orElseThrow();
        assertThat(secondA.originNodeId()).isEqualTo(NodeId.of("N04"));
        assertThat(secondA.destinationNodeId()).isEqualTo(NodeId.of("N03"));

        // Segunda llega al incidente pero HOSP-01 sigue ocupado por primera -> WAITING_HOSPITAL sin Tramo B
        assertThat(hospitalDispatchApplicationService.confirmArrivalAndReserveHospital(second.id().value()).outcome())
                .isEqualTo(HospitalDispatchOutcome.MOVED_TO_WAITING_HOSPITAL);
        assertThat(routeSegmentRepository.findByEmergencyIdAndType(second.id(), RouteSegmentType.INCIDENT_TO_HOSPITAL))
                .isEmpty();

        // Alta de primera libera HOSP-01 y reevalúa WAITING_HOSPITAL -> segunda pasa a IN_TRANSPORT y persiste Tramo B!
        deliveryAndDischargeApplicationService.executeSimulatedDischarge(firstDelivery.reservation().id());
        assertThat(emergencyRepository.findById(second.id()).orElseThrow().state())
                .isEqualTo(EmergencyState.IN_TRANSPORT);

        EmergencyRouteSegment secondB = routeSegmentRepository
                .findByEmergencyIdAndType(second.id(), RouteSegmentType.INCIDENT_TO_HOSPITAL)
                .orElseThrow();
        assertThat(secondB.resourceId()).isEqualTo("HOSP-01");
        assertThat(secondB.originNodeId()).isEqualTo(NodeId.of("N03"));
        assertThat(secondB.destinationNodeId()).isEqualTo(NodeId.of("N04"));
    }
}

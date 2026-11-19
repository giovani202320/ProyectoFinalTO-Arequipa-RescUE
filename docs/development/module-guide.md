# Guía de módulos de ARE

Resumen de responsabilidades y clases principales bajo `src/main/java/com/arequiparescue/`.

| Módulo | Responsabilidad | Clases clave |
|---|---|---|
| `emergency` | Registro de emergencias, máquina de 7 estados, triaje determinista y versionado inmutable de reglas (`RUL-VXX`) | `Emergency`, `DeterministicTriageService`, `EmergencyApplicationService`, `TriageRuleCatalogApplicationService`, `EmergencyController`, `TriageRuleCatalogController` |
| `ambulance` | Catálogo de ambulancias (`BASIC` / `SPECIALIZED`), estados operativos y bloqueo durante escenario activo | `Ambulance`, `AmbulanceId`, `AmbulanceCatalogApplicationService`, `AmbulanceCatalogController` |
| `hospital` | Catálogo hospitalario sin especialidades, invariante `C = R + O + D`, reservas, entregas y altas simuladas | `Hospital`, `HospitalId`, `HospitalReservation`, `HospitalCatalogApplicationService`, `HospitalDispatchApplicationService`, `HospitalDeliveryAndDischargeApplicationService`, `HospitalCatalogController`, `HospitalDispatchController` |
| `dispatch` | Elegibilidad de ambulancias, despacho supervisado, despacho automático con reintentos y cola `WAITING_AMBULANCE` | `AmbulanceEligibilityPolicy`, `WaitingAmbulanceQueuePolicy`, `DispatchApplicationService`, `DispatchController`, `WaitingQueueController`, `EligibleAmbulanceView` |
| `routing` | Grafo vial simplificado (`N01`–`N10`), costos abstractos positivos, algoritmo de Dijkstra y visualización esquemática | `RoadGraph`, `GraphNode`, `GraphEdge`, `DijkstraRouteCalculator`, `DijkstraPathResult`, `RoadGraphTopologyController` |
| `simulation` | Escenarios experimentales, bloqueo administrativo (`ActiveScenarioChecker`) y reloj lógico discreto | `SimulationScenario`, `DiscreteEventScheduler`, `SimulationController` |
| `reporting` | Catálogo de 7 métricas (`MET-01` a `MET-07`) y trazabilidad experimental | `ReportingCalculator`, `ReportingController` |
| `shared` | Tiempo lógico (`LogicalTime`), layout común Thymeleaf (`fragments/layout.html`, `components.html`) y formateo UI (`AreUiFormatter`) | `LogicalTime`, `AreUiFormatter`, `HomeController` |

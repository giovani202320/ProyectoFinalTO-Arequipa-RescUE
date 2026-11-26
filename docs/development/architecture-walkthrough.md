# Recorrido de arquitectura de ARE

Guía técnica interna para comprender cómo se implementa el **monolito modular hexagonal con Spring MVC + Thymeleaf** en **Arequipa RescUE (ARE)**.

---

## 1. Estilo arquitectónico y regla de dependencias

Cada módulo funcional bajo `src/main/java/com/arequiparescue/` se organiza en cuatro capas:

- **`domain/`**: entidades puras, objetos de valor, políticas de negocio y puertos de salida (interfaces de repositorio). No depende de Spring, JPA ni Servlet.
- **`application/`**: servicios de aplicación transaccionales (`@Transactional`), comandos, excepciones de validación y coordinación de casos de uso.
- **`infrastructure/`**: entidades JPA (`*JpaEntity`), repositorios Spring Data, adaptadores (`*JpaAdapter`) que implementan los puertos de dominio y mappers explícitos (`*PersistenceMapper`).
- **`presentation/`**: controladores Spring MVC (`@Controller`), DTO de formularios/vistas y plantillas Thymeleaf (`src/main/resources/templates/`).

---

## 2. Entidades de dominio frente a entidades JPA

- **Dominio puro (`Emergency`, `Ambulance`, `Hospital`, `RoadGraph`):** protegen sus invariantes en memoria y transiciones de estado (`Emergency.assignAmbulance(...)`, `Emergency.reserveHospitalAndStartTransport(...)`, `Hospital.updateCatalogConfiguration(...)`) sin anotaciones `@Entity` ni `@Column`.
- **Persistencia (`*JpaEntity` + `*PersistenceMapper`):** almacenan el estado relacional en MySQL 8.4 (por ejemplo, `EmergencyJpaEntity`, `HospitalJpaEntity`, `HospitalReservationJpaEntity`) y convierten explícitamente entre JPA y dominio dentro de cada `*JpaAdapter`.

---

## 3. Recorrido de una solicitud HTTP (ejemplo real: registro de emergencia `POST /emergencies`)

1. **Entrada MVC (`EmergencyController.registerEmergency`):** valida el formulario `@Valid @ModelAttribute("form")` (`EmergencyRegistrationForm`) y construye `RegisterEmergencyCommand`.
2. **Caso de uso (`EmergencyApplicationService`):** dentro de `@Transactional`, obtiene la versión activa de reglas de triaje (`TriageRuleVersion`), clasifica la categoría (`INC-A`..`INC-D`) mediante `DeterministicTriageService`, genera el siguiente identificador (`EMG-XXXXX`) y construye `Emergency.register(...)`.
3. **Persistencia (`EmergencyRepository` → `EmergencyJpaAdapter`):** mapea `Emergency` a `EmergencyJpaEntity` y persiste vía `SpringDataEmergencyRepository` en MySQL.
4. **Respuesta PRG:** `EmergencyController` agrega `justRegistered` como flash attribute y responde `redirect:/emergencies/{id}` (`HTTP 302`), evitando reenvíos al recargar (`ROB-01`).

---

## 4. Concurrencia, rutas y simulación

- **Rutas mínimas (`DijkstraRouteCalculator.shortestPath`):** servicio de dominio puro sobre `RoadGraph` que devuelve `Optional<DijkstraPathResult>` (`origin`, `destination`, `totalCost`, `path`), retornando vacío si un extremo está aislado (`N10`).
- **Despacho y colas FIFO (`DispatchApplicationService`, `HospitalDispatchApplicationService`):** evalúan `AmbulanceEligibilityPolicy` y `HospitalEligibilityPolicy`, aplican bloqueos pesimistas en BD (`findByIdForUpdate`) y gestionan las colas `WAITING_AMBULANCE` y `WAITING_HOSPITAL`.
- **Simulación discreta y métricas:** el reloj lógico (`LogicalTime`, ticks `t >= 0`) avanza manualmente en `/simulation`; `reporting` calcula las métricas experimentales `MET-01` a `MET-07` desde los registros persistidos.

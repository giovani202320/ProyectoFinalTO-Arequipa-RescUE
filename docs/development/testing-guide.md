# Guía de pruebas de ARE

ARE separa las pruebas rápidas de dominio/MVC de las pruebas de integración real contra MySQL 8.4 (`arequipa_rescue_test`).

---

## 1. Niveles de prueba

- **Dominio puro (`*DomainTest`, `DijkstraRouteCalculatorTest`, etc.):** validan invariantes, máquina de estados, políticas y algoritmo Dijkstra sin contexto de Spring ni base de datos.
- **Controladores MVC (`@WebMvcTest` con Mockito y `MockMvc`):** validan rutas HTTP, renderizado Thymeleaf, estructura única `appShell` y ausencia de IDs HTML duplicados (`HtmlStructureAssertions`). Ejemplos: `AdminAndRulesControllersTest`, `DispatchControllerTest`, `EmergencyControllerTest`, `HospitalDispatchControllerTest`, `SimulationControllerTest`, `ReportingControllerTest`.
- **Integración MySQL (`*MySqlIntegrationTest`):** ejecutan con `@ActiveProfiles("mysql-test")` y `@EnabledIfSystemProperty(named = "are.mysql.integration", matches = "true")` sobre **`arequipa_rescue_test`** (Flyway + JPA + concurrencia real).

---

## 2. Comandos de ejecución (PowerShell)

### Suite sin integración MySQL (omite los 29 tests de integración MySQL)

```powershell
.\mvnw.cmd clean test
```

### Suite completa con integración MySQL (`arequipa_rescue_test`)

Asegúrese de tener definida `$env:ARE_DB_PASSWORD` (y opcionalmente `$env:ARE_DB_PORT` o `$env:ARE_DB_TEST_URL`):

```powershell
.\mvnw.cmd test "-Dare.mysql.integration=true"
```

### Pruebas focalizadas de presentación

```powershell
.\mvnw.cmd test "-Dtest=AdminAndRulesControllersTest,DispatchControllerTest,EmergencyControllerTest"
```

---

## 3. Regla de aislamiento de bases de datos

- Las pruebas automatizadas **nunca** deben apuntar a `arequipa_rescue` ni a `arequipa_rescue_demo`.
- `src/test/resources/application-mysql-test.properties` apunta exclusivamente a `arequipa_rescue_test`.

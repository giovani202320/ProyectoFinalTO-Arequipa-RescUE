# ADR-0001: Stack tecnológico verificable, arquitectura de monolito modular hexagonal, persistencia con Flyway y estrategia de pruebas del MVP

- **Estado:** Aprobada (`DEC-020` — cierra `PEN-003`, `PEN-004`, `PEN-005` y `PEN-007`)
- **Fecha:** 2026-11-13
- **Proyecto:** ARE — Arequipa RescUE (`ProyectoFinalTO-Arequipa-RescUE`)
- **Decisiones vinculadas:** `DEC-001` a `DEC-020` (mecanismos técnicos de concurrencia reservados para `PEN-006`)

---

## 1. Contexto y problema

Con el cierre completo de las decisiones funcionales del MVP (`DEC-001` a `DEC-019`, cerrando `PEN-001` y `PEN-002a` a `PEN-002d`), el proyecto ARE requiere resolver en un único paquete técnico coherente cuatro definiciones previas al inicio de la construcción:

1. **`PEN-003` (Versiones exactas del stack tecnológico):** fijar versiones concretas, estables y compatibles de Java LTS, Spring Boot, Spring MVC, Thymeleaf, Spring Data JPA / Hibernate, MySQL, Flyway y Maven, verificando el entorno local disponible.
2. **`PEN-004` (Límites de módulos y arquitectura):** definir los módulos funcionales mínimos del monolito modular, sus responsabilidades, la estructura de paquetes bajo principios hexagonales pragmáticos y las reglas de dependencia entre capas y módulos.
3. **`PEN-005` (Estrategia de persistencia, transacciones y migraciones):** definir el esquema de evolución de base de datos sin `ddl-auto=update`, la estrategia de identificadores estables, las fronteras transaccionales de aplicación y la información mínima persistida, manteniendo la separación estricta entre dominio y entidades JPA (`DEC-003`) y sin adelantar las decisiones de bloqueo/aislamiento de `PEN-006`.
4. **`PEN-007` (Estrategia y herramientas de prueba):** establecer una pirámide de pruebas simple y ejecutable que cubra de forma prioritaria las invariantes funcionales aprobadas en `DEC-016` a `DEC-019`.

---

## 2. Restricciones y criterios

- Respetar estrictamente el stack base acordado en `AGENTS.md` (Sección 2.2) y `DEC-005`: Java, Spring Boot, Spring MVC, Thymeleaf, Spring Data JPA, Hibernate, MySQL y Maven.
- Mantener el paquete `domain/` 100 % libre de dependencias de Spring, JPA, Hibernate o Servlet (`AGENTS.md` Sección 4.1, `DEC-003`).
- Evitar múltiples proyectos Maven, microservicios, CQRS, Event Sourcing, brokers de mensajería o librerías que no aporten valor directo al MVP académico (`DEC-002`, `DEC-008`).
- Garantizar determinismo en los desempates por identificador técnico estable (`RN-ASI-005`, `RN-HOS-006`, `RN-HOS-010`, `RN-RUT-009`).
- No resolver por suposición los mecanismos técnicos de concurrencia (`PEN-006`: bloqueo optimista `@Version` vs. pesimista, niveles de aislamiento, retry técnico y pruebas multihilo).

---

## 3. Alternativas consideradas

### 3.1 Stack y construcción (`PEN-003`)
- **Alternativa A (Adoptada):** **Java `21` LTS** (verificado en el entorno local: `21.0.10 LTS`), **Spring Boot `3.4.5`** (gestión unificada por BOM para Spring MVC `6.2.x`, Thymeleaf `3.1.3.RELEASE`, Spring Data JPA `3.4.x` / Hibernate `6.6.x` y Flyway `10.20.x`), **MySQL `8.4` LTS** (`mysql-connector-j`) y **Apache Maven `3.9.9`** mediante **Maven Wrapper (`mvnw`)** en un único proyecto Maven.
- **Alternativa B (Descartada):** Múltiples submódulos Maven por capa o módulo funcional, o uso de versiones no LTS / experimentales. Se descartó porque añade fricción de compilación y mantenimiento innecesaria para el MVP académico.

### 3.2 Organización arquitectónica (`PEN-004`)
- **Alternativa A (Adoptada):** Monolito modular en un solo artefacto desplegable con **7 módulos funcionales cohesivos** (`emergency`, `ambulance`, `dispatch`, `routing`, `hospital`, `simulation`, `reporting`), cada uno con separación interna `domain`, `application`, `infrastructure` y `presentation` (cuando exponga vistas/controladores), usando puertos y adaptadores únicamente en las fronteras que requieren inversión de dependencias (persistencia y contratos entre módulos).
- **Alternativa B (Descartada):** Empaquetado horizontal único por capas globales (`controllers/`, `services/`, `repositories/`) o arquitectura sobre-ingenierizada con buses de eventos/CQRS. Se descartó porque el empaquetado puramente horizontal diluye las fronteras de negocio, mientras que CQRS/buses contradicen la simplicidad aprobada para el MVP.

### 3.3 Persistencia y migraciones (`PEN-005`)
- **Alternativa A (Adoptada):** Entidades JPA en `infrastructure` separadas del dominio puro con mapeadores explícitos, identificadores técnicos de negocio estables (`VARCHAR(32)` con formato canónico ordenable como `AMB-01`, `HOSP-01`, `N01`, `EMG-00001`), migraciones SQL versionadas con **Flyway** y validación estricta del esquema con `spring.jpa.hibernate.ddl-auto=validate`.
- **Alternativa B (Descartada):** Uso de `ddl-auto=update` sin migraciones versionadas o acoplamiento de anotaciones `@Entity` sobre las clases de `domain/`. Se descartó porque `ddl-auto=update` no es reproducible ni auditable y anotar el dominio viola `DEC-003`.

### 3.4 Estrategia de pruebas (`PEN-007`)
- **Alternativa A (Adoptada):** Suite centrada en `spring-boot-starter-test` (**JUnit 5**, **AssertJ**, **Mockito**, **MockMvc**, **`@DataJpaTest`**), priorizando pruebas unitarias rápidas y sin Spring para todo el dominio (`DEC-016` a `DEC-019`) y pruebas focalizadas de casos de uso y persistencia sobre MySQL.
- **Alternativa B (Descartada):** Incorporar frameworks E2E pesados de navegador (Selenium/Playwright) o herramientas externas de carga como requisito base del MVP. Se descartó por sobrecosto de configuración sin beneficio directo para las invariantes del simulador.

---

## 4. Decisión

### 4.1 Versiones aprobadas del stack (`PEN-003`)

| Componente | Versión aprobada | Verificación en entorno / Gestión de versión |
| :--- | :--- | :--- |
| **Java** | **Java `21` LTS** (`21.0.10` verificada localmente) | Instalado y verificado en el entorno (`java` y `javac 21.0.10`). |
| **Spring Boot** | **`3.4.5`** | BOM oficial (`spring-boot-starter-parent:3.4.5`), soporte nativo para Java 21. |
| **Spring MVC** | **`6.2.x`** | Gestionado por el BOM de Spring Boot `3.4.5` (`spring-boot-starter-web`, `spring-boot-starter-validation`). |
| **Thymeleaf** | **`3.1.3.RELEASE`** | Gestionado por el BOM de Spring Boot `3.4.5` (`spring-boot-starter-thymeleaf`). |
| **Spring Data JPA / Hibernate** | **Spring Data JPA `3.4.x` / Hibernate ORM `6.6.x`** | Gestionado por el BOM de Spring Boot `3.4.5` (`spring-boot-starter-data-jpa`). |
| **Migraciones SQL** | **Flyway `10.20.x`** (`flyway-core`, `flyway-mysql`) | Gestionado por el BOM de Spring Boot `3.4.5`. |
| **Base de datos y Driver** | **MySQL `8.4` LTS** (compatible con `8.0+`) y `com.mysql:mysql-connector-j` | Soportado mediante instancia local o contenedor Docker (`docker.exe` verificado en el entorno). |
| **Construcción** | **Apache Maven `3.9.9`** vía **Maven Wrapper (`mvnw`)** | Se incluirá `mvnw` / `mvnw.cmd` en la raíz del repositorio dado que `mvn` no está en el `PATH` global. |

### 4.2 Módulos funcionales y reglas de dependencia (`PEN-004`)

El paquete raíz será `com.arequiparescue`, organizado en **7 módulos funcionales** dentro de un único proyecto Maven:

```text
com.arequiparescue
├── shared/          # Tipos transversales puros (tiempo lógico, IDs base, excepciones de dominio)
├── emergency/       # Registro, catálogo INC-A..D, clasificación determinista, versiones de reglas y estados de emergencia
├── ambulance/       # Catálogo BASIC/SPECIALIZED, ubicación en grafo y estados de ambulancia
├── dispatch/        # Elegibilidad, preservación, asignación supervisada/automática, reintentos DEC-017 y cola WAITING_AMBULANCE
├── routing/         # Grafo simplificado inicial de 10 nodos (N01..N10) y algoritmo de Dijkstra
├── hospital/        # Capacidad C/R/O/D, selección post-recogida, reserva concurrente DEC-018, cola WAITING_HOSPITAL y alta simulada
├── simulation/      # Motor de eventos discretos con avance manual, tiempo lógico monotónico y control de escenarios activos
└── reporting/       # Registro experimental trazable (RN-MET-003) y catálogo de 7 métricas (RN-MET-004)
```

**Responsabilidades y dependencias permitidas:**
1. **Hacia el interior dentro de cada módulo:** `presentation` $\to$ `application` $\to$ `domain` $\leftarrow$ `infrastructure`.
2. **Independencia del dominio:** ningún archivo bajo `..domain..` puede importar `org.springframework..`, `jakarta.persistence..`, `org.hibernate..` ni `jakarta.servlet..`.
3. **Comunicación entre módulos:** se realiza exclusivamente a través de contratos de `application` o tipos de `domain`/`shared`. **Queda prohibido que un módulo acceda a las clases de `infrastructure` o `presentation` de otro módulo.**
4. **Ubicación pragmática de puertos:** las interfaces de repositorio necesarias para persistir agregados del dominio se ubican en `domain/port/` (o `application/port/` para puertos de consulta/coordinación de casos de uso), y sus implementaciones JPA residen en `infrastructure/persistence/`.

### 4.3 Persistencia, transacciones y migraciones (`PEN-005`)

1. **Separación de modelos y mapeo:** cada tabla relacional es representada en `infrastructure` por una clase `*JpaEntity` independiente de las clases de `domain`. Los adaptadores (`*PersistenceAdapter`) traducen explícitamente entre ambos modelos preservando todas las invariantes (`R + O <= C`, estados válidos, inmutabilidad de versiones publicadas).
2. **Estrategia de identificadores estables:** se utilizan identificadores técnicos de negocio estables y ordenables (`VARCHAR(32)` con restricción `UNIQUE` o clave primaria natural controlada: `N01`..`N10`, `AMB-01`, `HOSP-01`, `EMG-00001`, `RUL-V01`, `SCN-001`, `EVT-00001`), garantizando orden determinista en los desempates aprobados en `DEC-018` y `DEC-019`.
3. **Política de esquema y migraciones:**
   - Evolución controlada mediante scripts SQL versionados de **Flyway** en `src/main/resources/db/migration/` (`V1__init_schema.sql`, `V2__seed_initial_graph_and_rules.sql`).
   - Configuración obligatoria `spring.jpa.hibernate.ddl-auto=validate` en los perfiles de ejecución sobre MySQL; se prohíbe `ddl-auto=update`.
4. **Fronteras transaccionales:** cada caso de uso de `application` que modifica recursos se ejecuta dentro de una transacción explícita (`@Transactional`), mientras que las consultas operan con `@Transactional(readOnly = true)`.
5. **Información mínima persistida:**
   - **Versiones de reglas de clasificación:** identificador de versión, estado (`DRAFT`/`PUBLISHED`/`ACTIVE`), fecha/tiempo lógico de publicación y reglas por categoría (`INC-A` a `INC-D` $\to$ prioridad informativa y equipamiento requerido).
   - **Grafo:** nodos (`N01` a `N10`) y aristas ponderadas con costos enteros positivos abstractos.
   - **Ambulancias:** identificador estable, equipamiento (`BASIC`/`SPECIALIZED`), nodo actual, estado (`AVAILABLE`, `ASSIGNED`, `AT_INCIDENT`, `TRANSPORTING`) y referencia a emergencia asignada.
   - **Hospitales:** identificador estable, nodo de ubicación, estado `enabled`, capacidad total `C`, plazas reservadas `R` y plazas ocupadas `O` (con `CHECK (R + O <= C)`).
   - **Emergencias:** identificador estable, categoría, nodo de ubicación, descripción opcional, prioridad simulada, equipamiento requerido, versión de reglas aplicada, estado (`REGISTERED` a `COMPLETED`), ambulancia asignada, hospital reservado/ocupado, marcas de tiempo lógico de cada transición y motivo funcional del último resultado.
   - **Reservas y ocupaciones hospitalarias:** identificador de reserva/ocupación, emergencia asociada, hospital asociado, estado (`RESERVED`, `OCCUPIED`, `DISCHARGED`) y marcas de tiempo lógico.
   - **Escenarios, eventos discretos y bitácora experimental:** escenario experimental (estado `ACTIVE`/`COMPLETED` y tiempo lógico actual $t$), cola de eventos discretos (tiempo lógico $t$, tipo de evento, secuencia monotónica y estado) y registro de intentos/conflictos/reintentos (`CONCURRENCY_RETRY_EXHAUSTED`, `HOSPITAL_RESERVATION_RETRY_EXHAUSTED`).
6. **Reserva expresa para `PEN-006`:** la elección específica entre bloqueo optimista (`@Version`) y bloqueo pesimista (`SELECT ... FOR UPDATE`), los niveles de aislamiento transaccional, el mecanismo técnico de reintentos y la coordinación concurrente de colas permanecen pendientes en **`PEN-006`**.

### 4.4 Estrategia de pruebas (`PEN-007`)

1. **Pruebas unitarias de dominio (JUnit 5 + AssertJ, sin contexto Spring ni BD):** verifican de forma aislada y determinista:
   - Clasificación simulada `DEC-016` (`INC-A` a `INC-D`, equipamiento `BASIC`/`SPECIALIZED`, inmutabilidad de versiones y bloqueo de activación en escenario activo).
   - Algoritmo de Dijkstra sobre el grafo de 10 nodos (`N01`–`N10`), rutas alternativas, nodo inalcanzable `N10` y desempates deterministas (`RN-ASI-005`, `RN-HOS-006`).
   - Elegibilidad, preservación de ambulancias especializadas y ordenamiento FIFO de colas al liberar unidades `SPECIALIZED` y `BASIC`.
   - Máquina de estados de emergencia (7 estados) y ambulancia (4 estados), retención de ambulancia en `WAITING_HOSPITAL` y liberación a la llegada al hospital (`DEC-019`).
   - Invariantes de capacidad hospitalaria (`C`, `R`, `O`, `D`), transformación de reserva en ocupación y alta simulada sin duplicidad (`DEC-018`).
   - Cola de eventos discretos con tiempo lógico monotónico y desempate determinista, y cálculo de las 7 métricas (`DEC-019`).
2. **Pruebas de casos de uso en `application` (JUnit 5 + Mockito):** validan la coordinación de puertos, reintentos funcionales (`DEC-017`, `DEC-018`) y registro experimental (`RN-MET-003`).
3. **Pruebas de integración de persistencia y web (`@DataJpaTest`, `@WebMvcTest` / `MockMvc`):** validan las migraciones Flyway, los mapeadores explícitos entre entidades de dominio y JPA, las restricciones de integridad relacional y los controladores Spring MVC.
4. **Pruebas de concurrencia (`DEC-017` y `DEC-018`):** se diseñarán y ejecutarán al resolver **`PEN-006`**.

---

## 5. Consecuencias

- **Positivas:** Quedan cerrados `PEN-003`, `PEN-004`, `PEN-005` y `PEN-007` con una arquitectura limpia, simple y alineada con el entorno Java 21 LTS local, habilitando la futura inicialización del proyecto cuando se autorice.
- **Compromisos y pendientes abiertos:**
  - **`PEN-006`** continúa abierto (mecanismos técnicos de concurrencia, bloqueos, aislamiento y pruebas multihilo).
  - **`PEN-008`** continúa abierto (autorización para alinear las rutas ilustrativas de las Secciones 8.2 y 8.3 de `AGENTS.md`).

---

## 6. Evidencia y criterios de verificación

- Verificación del entorno local: `java -version` y `javac -version` reportan `21.0.10 LTS`; `docker.exe` reporta presencia en `C:\Program Files\Docker\Docker\resources\bin\docker.exe`.
- Cuando se autorice la creación del esqueleto del proyecto, se verificará que `domain/` no contenga imports de Spring/JPA/Hibernate, que Flyway valide el esquema con `ddl-auto=validate` y que las suites de JUnit 5 ejecuten las pruebas de las reglas `DEC-016` a `DEC-019`.

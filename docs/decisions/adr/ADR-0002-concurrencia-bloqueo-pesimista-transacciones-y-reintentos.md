# ADR-0002: Control de concurrencia mediante transacciones cortas, bloqueo pesimista en InnoDB y coordinación determinista de reintentos

- **Estado:** Aprobada (`DEC-021` — cierra `PEN-006`)
- **Fecha:** 2026-11-13
- **Proyecto:** ARE — Arequipa RescUE (`ProyectoFinalTO-Arequipa-RescUE`)
- **Decisiones vinculadas:** `DEC-011`, `DEC-012`, `DEC-017`, `DEC-018`, `DEC-019`, `DEC-020` (`ADR-0001`), `DEC-021`

---

## 1. Contexto y problema

Con las políticas funcionales de concurrencia ya aprobadas en `DEC-017` (reintentos automáticos en asignación de ambulancias: 1 intento inicial + hasta 3 reintentos) y `DEC-018` (reintentos en reserva hospitalaria: 1 intento inicial + hasta 2 reintentos), y con el stack y esquema base fijados en `DEC-020` (`ADR-0001`: Java 21 LTS, Spring Boot 3.4.5, Spring Data JPA / Hibernate 6.6, MySQL 8.4 LTS con motor InnoDB y Flyway), resta resolver **`PEN-006`**: definir el mecanismo técnico concreto de bloqueo, aislamiento transaccional, detección de conflictos, prevención de interbloqueos (*deadlocks*), persistencia sin duplicados de intentos y verificación multihilo para el MVP académico.

---

## 2. Restricciones y criterios

- Utilizar exclusivamente **MySQL 8.4 (InnoDB)** y **Spring Data JPA** sin introducir colas distribuidas, Kafka, Redis, microservicios ni infraestructura externa (`AGENTS.md` Secciones 2.4 y 6.3).
- Evitar la combinación innecesaria de bloqueo optimista y pesimista sobre una misma operación (`AGENTS.md` Sección 6.3).
- No clasificar automáticamente cualquier excepción SQL como conflicto concurrente reintentable (`RN-CON-005`, `RN-HOS-008`).
- Mantener estrictamente independientes los contadores de reintentos entre asignación de ambulancias (`DEC-017`, máximo 4 intentos) y reserva hospitalaria (`DEC-018`, máximo 3 intentos).
- Preservar el orden FIFO, los desempates deterministas por identificador estable y la separación entre el tiempo lógico de simulación ($t \ge 0$) y la duración física de las transacciones SQL.

---

## 3. Alternativas consideradas

### 3.1 Estrategia de bloqueo sobre recursos compartidos
- **Alternativa A (Adoptada): Transacciones cortas por intento (`REQUIRES_NEW`, `READ_COMMITTED`) con bloqueo pesimista de fila (`SELECT ... FOR UPDATE` / `LockModeType.PESSIMISTIC_WRITE`) y restricciones SQL de integridad.** Serializa en InnoDB el instante exacto de confirmación sobre la fila de la ambulancia o del hospital candidato, permite verificar inmediatamente bajo el bloqueo si el recurso sigue disponible (`status = 'AVAILABLE'` o `D >= 1`) y mantiene una semántica determinista y directa de depurar en pruebas académicas.
- **Alternativa B (Descartada): Bloqueo exclusivamente optimista (`@Version`) en todas las tablas operativas o combinación simultánea de `@Version` y `SELECT ... FOR UPDATE`.** Se descartó porque combinar ambos mecanismos en la misma operación contradice `AGENTS.md` (Sección 6.3) y añade complejidad de sincronización del contexto de persistencia de Hibernate tras `OptimisticLockException`.

### 3.2 Ejecución del bucle de reintentos
- **Alternativa A (Adoptada): Coordinador de aplicación sin transacción envolvente que invoca por cada intento un método transaccional corto (`Propagation.REQUIRES_NEW`, `Isolation.READ_COMMITTED`).** Cada intento opera sobre un `EntityManager` limpio y lee los commits más recientes de otros hilos para recalcular elegibilidad, preservación y Dijkstra (`RN-CON-006`, `RN-HOS-008`).
- **Alternativa B (Descartada): Ejecutar los reintentos dentro de una única transacción `REPEATABLE_READ`.** Se descartó porque bajo la lectura repetible por instantánea (*consistent snapshot*) de InnoDB los reintentos no observarían los cambios confirmados por transacciones concurrentes o retendrían bloqueos durante múltiples iteraciones.

---

## 4. Decisión

Se aprueba la siguiente arquitectura técnica mínima de concurrencia para los diez puntos de **`PEN-006`**:

| # | Ámbito | Decisión técnica adoptada |
| :---: | :--- | :--- |
| **1** | **Prevención de doble asignación de ambulancia** | El coordinador evalúa candidatas elegibles y, al intentar confirmar la unidad seleccionada (`AMB-XX`), abre una transacción corta que adquiere bloqueo pesimista exclusivo (`@Lock(LockModeType.PESSIMISTIC_WRITE)` $\to$ `SELECT ... FROM ambulances WHERE id = :id FOR UPDATE`). Bajo el bloqueo verifica `status = 'AVAILABLE'` y `assigned_emergency_id IS NULL`. Si otra transacción la tomó antes, no modifica la fila y devuelve `AMBULANCE_UNAVAILABLE_CONFLICT`. Adicionalmente, la tabla `ambulances` incluye restricción `UNIQUE (assigned_emergency_id)` y `CHECK` de estado coherente. |
| **2** | **Protección de la última plaza hospitalaria (`D = 1`)** | Al intentar reservar plaza en el hospital seleccionado (`HOSP-XX`), la transacción corta ejecuta `SELECT ... FROM hospitals WHERE id = :id FOR UPDATE` (`PESSIMISTIC_WRITE`). Bajo el bloqueo exclusivo evalúa `enabled = TRUE` y `D = capacity_total - reserved_slots - occupied_slots >= 1`. Solo un hilo puede incrementar `reserved_slots = reserved_slots + 1` cuando `D = 1`; el hilo concurrente observa `D = 0` al adquirir el bloqueo y devuelve `HOSPITAL_CAPACITY_CONFLICT`. La tabla `hospitals` refuerza la invariante con `CHECK (reserved_slots >= 0 AND occupied_slots >= 0 AND reserved_slots + occupied_slots <= capacity_total)` y `hospital_reservations` impide más de una reserva/ocupación activa por emergencia. |
| **3** | **Confirmación o reversión de operaciones atómicas** | Se separa el **Coordinador de Operación** (sin transacción larga) del **Ejecutor de Intento Atómico** anotado con `@Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED, timeout = 5)`. Si la validación bajo bloqueo tiene éxito, actualiza en la misma transacción corta la emergencia y el recurso (`Ambulance` o `Hospital` + `HospitalReservation`) y ejecuta `COMMIT`. Si ocurre conflicto o excepción, la transacción del intento se revierte (`ROLLBACK`) sin dejar estados intermedios ni recursos parcialmente tomados (`RN-CON-009`). |
| **4** | **Detección y clasificación de conflictos concurrentes** | Solo se clasifican como **conflicto concurrente reintentable** dos casos: **(a)** pérdida de disponibilidad detectada explícitamente tras adquirir `SELECT ... FOR UPDATE` (`status != 'AVAILABLE'` en ambulancia o `D < 1` / `enabled = FALSE` en hospital), y **(b)** excepciones transaccionales de contención de bloqueo traducidas por Spring (`PessimisticLockingFailureException`, `CannotAcquireLockException`, `DeadlockLoserDataAccessException`, equivalentes a MySQL `1213` / `1205`). Cualquier otra excepción (`DataIntegrityViolationException` por datos inválidos, sintaxis SQL, caída de conexión o error de validación) se clasifica como **error técnico no reintentable** y aborta de inmediato sin consumir reintentos de concurrencia. |
| **5** | **Aplicación independiente de reintentos (`DEC-017` y `DEC-018`)** | **Asignación automática (`DEC-017`):** bucle en `dispatch/application` con contador local `attempt = 1..4` (1 intento inicial + hasta 3 reintentos). Antes de cada intento consulta el estado fresco (`READ_COMMITTED`) y recalcula disponibilidad, compatibilidad `BASIC`/`SPECIALIZED`, preservación y Dijkstra. Si no hay elegibles en `attempt = 1`, pasa a `WAITING_AMBULANCE` con `0` reintentos; si tras un conflicto ya no quedan elegibles, termina anticipadamente sin consumir reintentos vacíos; si fallan los 4 intentos por conflicto concurrente, persiste `CONCURRENCY_RETRY_EXHAUSTED`, deja la emergencia en `WAITING_AMBULANCE` y activa el indicador de aviso visible para el `Despachador`. En **modo supervisado** (`maxAttempts = 1`), ante conflicto no reintenta sino que recarga alternativas elegibles (`RN-CON-002`). **Reserva hospitalaria (`DEC-018`):** bucle independiente en `hospital/application` con contador local `attempt = 1..3` (1 intento inicial + hasta 2 reintentos), reevaluación completa en cada intento, finalización anticipada y transición a `WAITING_HOSPITAL` con `HOSPITAL_RESERVATION_RETRY_EXHAUSTED` al agotar los 3 intentos. |
| **6** | **Preservación de FIFO y desempates deterministas** | Las colas se consultan directamente en MySQL mediante índices compuestos deterministas: `ORDER BY queue_entered_at_t ASC, id ASC` para `WAITING_AMBULANCE` (filtrando por grupo `SPECIALIZED` y luego `BASIC` según `RN-COL-001`, `RN-COL-002` y `RN-COL-007`) y `ORDER BY hospital_queue_entered_at_t ASC, id ASC` para `WAITING_HOSPITAL` (`RN-HOS-010`). Si una emergencia en `WAITING_HOSPITAL` no tiene hospital alcanzable/elegible durante una reevaluación (`CA-HOS-09`), no se modifica su marca `hospital_queue_entered_at_t`, conservando su posición FIFO mientras se evalúa la siguiente fila. Los desempates de ruta usan orden lexicográfico ascendente sobre IDs canónicos (`AMB-01` < `AMB-02`, `HOSP-01` < `HOSP-02`). |
| **7** | **Prevención de interbloqueos (*deadlocks*)** | Toda transacción que adquiera más de un bloqueo pesimista respeta un **orden global único e invariable de adquisición de recursos**: **1.°** `simulation_scenarios` (si aplica) $\to$ **2.°** `emergencies` (orden `id ASC`) $\to$ **3.°** `ambulances` (orden `id ASC`) $\to$ **4.°** `hospitals` (orden `id ASC`). Además, cada intento bloquea a lo sumo una emergencia y un único recurso candidato (una ambulancia o un hospital), eliminando esperas circulares. |
| **8** | **Registro de intentos, conflictos y resultados sin duplicados** | Cada operación de asignación o reserva genera un `operation_id` único y numera sus intentos (`attempt_number` `1..4` o `1..3`). El registro experimental (`operation_attempt_logs` / `experiment_operation_results`) aplica restricciones `UNIQUE (operation_id, attempt_number)` y `UNIQUE (operation_id)` y se persiste en transacción independiente (`REQUIRES_NEW`), garantizando que un `ROLLBACK` del intento fallido no borre la evidencia del conflicto ni produzca registros duplicados (`RN-CON-008`, `RN-MET-003`). En el **alta simulada (`CA-HOS-11`)**, se bloquea la fila de ocupación con `SELECT ... FOR UPDATE` y solo se procesa si `status = 'OCCUPIED'`, rechazando de forma idempotente cualquier invocación duplicada. |
| **9** | **Separación entre tiempo lógico ($t$) y transacciones reales** | El tiempo lógico ($t \ge 0$) es un entero monotónico de dominio almacenado en el escenario y avanzado únicamente por pasos manuales del motor de eventos discretos (`RN-RUT-009`). Las transacciones SQL duran milisegundos en tiempo físico, leen el valor vigente de $t$ como dato de entrada para sellar las transiciones y liberan inmediatamente las conexiones y bloqueos de InnoDB; jamás se mantienen transacciones abiertas esperando interacción del usuario ni avance del reloj simulado. |
| **10** | **Verificación mediante pruebas multihilo contra MySQL** | Las pruebas de concurrencia se ejecutan con JUnit 5 (`@SpringBootTest`) sobre una instancia real de **MySQL 8.4 (InnoDB)** con esquema Flyway validado, utilizando `ExecutorService` y barreras `CountDownLatch` (`readyLatch` + `startLatch`) para disparar hilos simultáneos deterministas que verifican: **(a)** ausencia de doble asignación sobre una misma ambulancia (`CA-CON-07`), **(b)** consumo de 1 a 3 reintentos y agotamiento al 4.º intento con `CONCURRENCY_RETRY_EXHAUSTED` (`CA-CON-02`, `CA-CON-06`), **(c)** protección de la última plaza hospitalaria (`D = 1`) e invariante `R + O <= C` bajo contención con agotamiento al 3.er intento `HOSPITAL_RESERVATION_RETRY_EXHAUSTED` (`CA-HOS-04`, `CA-HOS-05`), y **(d)** no duplicidad de alta simulada concurrente sobre la misma ocupación (`CA-HOS-11`). |

---

## 5. Consecuencias

- **Positivas:**
  - Queda cerrado **`PEN-006`** con un diseño técnico simple, estándar en Spring Data JPA + MySQL InnoDB y directamente trazable a los criterios `CA-CON-01` a `CA-CON-10`, `CA-HOS-01` a `CA-HOS-13` y `CA-SIM-01` a `CA-SIM-10`.
  - Se evitan bloqueos optimistas y pesimistas mezclados y se elimina el riesgo de que transacciones fallidas dejen recursos bloqueados o pierdan la trazabilidad de los intentos.
- **Compromisos y pendientes abiertos:**
  - Con el cierre de `PEN-006`, **todos los pendientes funcionales y técnicos de diseño (`PEN-001` a `PEN-007`) están cerrados**.
  - Únicamente permanece abierto **`PEN-008`** (autorización expresa para actualizar las rutas ilustrativas de las Secciones 8.2 y 8.3 de `AGENTS.md`).

---

## 6. Evidencia y criterios de verificación

- Durante la implementación del MVP, las pruebas de integración multihilo sobre MySQL 8.4 verificarán empíricamente que ante $N$ hilos sincronizados con `CountDownLatch` sobre una única ambulancia disponible o un hospital con `D = 1`, el número de asignaciones/reservas confirmadas sobre ese recurso es exactamente `1`, se preserva `R + O <= C`, se registran sin duplicados los intentos y códigos `CONCURRENCY_RETRY_EXHAUSTED` / `HOSPITAL_RESERVATION_RETRY_EXHAUSTED` y no se producen interbloqueos no controlados.

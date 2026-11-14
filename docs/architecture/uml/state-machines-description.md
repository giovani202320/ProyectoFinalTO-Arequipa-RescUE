# Especificación de las Máquinas de Estados UML del MVP — ARE (Arequipa RescUE)

**Estado del documento:** Propuesta técnica alineada con `DEC-018`, `DEC-019`, `DEC-020` (`ADR-0001`), `DEC-021` (`ADR-0002`) y `business-rules.md` — Pendiente de revisión humana antes de commit
**Diagramas PlantUML asociados:**
- [`emergency-states.puml`](file:///c:/Users/giovani/Documents/GitHub/ProyectoFinalTO-Arequipa-RescUE/docs/architecture/uml/emergency-states.puml)
- [`ambulance-states.puml`](file:///c:/Users/giovani/Documents/GitHub/ProyectoFinalTO-Arequipa-RescUE/docs/architecture/uml/ambulance-states.puml)

**Propósito:** Formalizar las máquinas de estados de **Emergencia** y **Ambulancia** del simulador académico ARE para guiar la implementación del dominio puro, la coordinación transaccional de eventos discretos y las pruebas automatizadas JUnit 5.

---

## 1. Estados autorizados (`DEC-019` / `RN-RUT-007`)

### 1.1 Estados de Emergencia (exactamente 7 estados)

| Estado | Significado en el dominio | Estado simultáneo de la ambulancia activamente vinculada (`assigned_emergency_id == emergency.id`) |
| :--- | :--- | :--- |
| **`REGISTERED`** | Emergencia registrada y clasificada determinísticamente con prioridad informativa, equipamiento requerido (`BASIC` o `SPECIALIZED`) y referencia inmutable a la versión activa de reglas (`RN-EME-009`). | Sin ambulancia activamente vinculada (`ninguna unidad con assigned_emergency_id == emergency.id`) |
| **`WAITING_AMBULANCE`** | Emergencia en cola FIFO de espera de ambulancia por ausencia inicial de unidades elegibles o tras agotar los `1+3` intentos en asignación automática (`RN-ASI-004`, `RN-CON-007`). | Sin ambulancia activamente vinculada (`ninguna unidad con assigned_emergency_id == emergency.id`) |
| **`ASSIGNED`** | Ambulancia confirmada y desplazándose por la ruta calculada hacia el nodo del incidente. | La ambulancia con `assigned_emergency_id == emergency.id` está en **`ASSIGNED`** |
| **`AT_INCIDENT`** | Ambulancia en el nodo del incidente con recogida confirmada, ejecutando la selección y reserva hospitalaria (`RN-HOS-005`). | La ambulancia con `assigned_emergency_id == emergency.id` está en **`AT_INCIDENT`** |
| **`WAITING_HOSPITAL`** | Emergencia en cola FIFO de espera de destino hospitalario por ausencia de hospitales elegibles (`D >= 1` y alcanzables) o agotamiento de los `1+2` intentos de reserva (`RN-HOS-009`). Conserva su ambulancia retenida en el incidente. | La ambulancia con `assigned_emergency_id == emergency.id` está en **`AT_INCIDENT`** *(retenida y ocupada)* |
| **`IN_TRANSPORT`** | Destino hospitalario y reserva de plaza (`R = R + 1`) confirmados (`RN-HOS-007`); ambulancia trasladando la emergencia hacia el nodo del hospital. | La ambulancia con `assigned_emergency_id == emergency.id` está en **`TRANSPORTING`** |
| **`COMPLETED`** | Llegada al hospital reservado y entrega simulada completada (`RN-EME-001`, `RN-HOS-012`). Estado terminal de la emergencia. | Desvinculada inmediatamente (`assigned_emergency_id = NULL`, la unidad pasa a **`AVAILABLE`**) |

### 1.2 Estados de Ambulancia (exactamente 4 estados)

| Estado | Significado en el dominio | Estado de la emergencia activamente vinculada (`assigned_emergency_id`) |
| :--- | :--- | :--- |
| **`AVAILABLE`** | Disponible en su nodo actual (`N01`–`N10`) para evaluación de elegibilidad y despacho. | Sin emergencia vinculada (`assigned_emergency_id = NULL`) |
| **`ASSIGNED`** | Asignada de forma exclusiva a una emergencia activa y desplazándose hacia el nodo del incidente. | La emergencia referenciada por `assigned_emergency_id` está en **`ASSIGNED`** |
| **`AT_INCIDENT`** | En el nodo del incidente durante la recogida y reserva hospitalaria, o retenida junto a su emergencia vinculada mientras esta espera en la cola hospitalaria. | La emergencia referenciada por `assigned_emergency_id` está en **`AT_INCIDENT`** o **`WAITING_HOSPITAL`** |
| **`TRANSPORTING`** | Trasladando a su emergencia vinculada hacia el hospital cuyo destino y plaza reservada (`R`) fueron confirmados. | La emergencia referenciada por `assigned_emergency_id` está en **`IN_TRANSPORT`** |

> [!IMPORTANT]
> **Exclusiones estrictas de estados (`DEC-019`):**
> - No existen en Emergencia estados como `TRIAGED`, `DISPATCHED`, `EN_ROUTE_TO_INCIDENT` ni `CANCELLED`.
> - No existen en Ambulancia estados como `EN_ROUTE`, `DISPATCHED`, `WAITING_HOSPITAL` ni `MAINTENANCE`.
> - Los estados del registro de reserva/ocupación hospitalaria (`RESERVED`, `OCCUPIED`, `DISCHARGED` en `ADR-0001` Secc. 4.3.5) pertenecen a la entidad de relación hospitalaria y nunca son estados de la emergencia ni de la ambulancia.

---

## 2. Tablas de transiciones, eventos, condiciones y acciones

### 2.1 Transiciones de la Máquina de Estados de Emergencia (`emergency-states.puml`)

| ID | Estado origen | Estado destino | Evento desencadenante | Condición (Guarda) | Acción atómica y efectos en el tiempo lógico ($t$) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`T-EME-00`** | `[*]` | `REGISTERED` | `registrarEmergencia(categoria, nodo, desc)` (`HU-01`) | Categoría `INC-A`..`INC-D` activa, nodo en `N01`..`N10` y versión activa de reglas válida (`RN-EME-010`). | Determina prioridad informativa y equipamiento (`BASIC`/`SPECIALIZED`), asocia `rule_version_id` inmutable y sella `registered_at_t = t`. |
| **`T-EME-01`** | `REGISTERED` | `ASSIGNED` | `evaluarYAsignarAmbulancia()` (`HU-06`, `HU-07`) | Ambulancia elegible confirmada bajo `SELECT ... FOR UPDATE` (`status = 'AVAILABLE'`, `assigned_emergency_id IS NULL`), en intento único (supervisado) o dentro de `1..4` intentos (automático). | Vincula la unidad (`assigned_emergency_id = emergency.id`, `assigned_ambulance_id`), ejecuta `T-AMB-01` (`AVAILABLE -> ASSIGNED`) en la misma transacción corta (`REQUIRES_NEW`), agenda arribo al incidente y sella `assigned_at_t = t`. |
| **`T-EME-02`** | `REGISTERED` | `WAITING_AMBULANCE` | `evaluarYAsignarAmbulancia()` (`HU-01`, `HU-06`, `HU-07`) | Sin ambulancia elegible en el intento inicial (`0` reintentos) **o** fallo por conflicto concurrente en los `4` intentos automáticos (`1+3`). | Sella `queue_entered_at_t = t`; si agotó los `1+3` intentos automáticos, registra `CONCURRENCY_RETRY_EXHAUSTED` y activa aviso visible al `Despachador` (`CA-CON-06`). |
| **`T-EME-03`** | `WAITING_AMBULANCE` | `ASSIGNED` | `atenderColaEsperaAmbulancia()` (`HU-08`) / selección supervisada (`HU-06`) | Selección bajo distinción estricta entre: **(a)** compatibilidad y preservación de equipamiento (al liberarse unidad `SPECIALIZED`: evalúa primero el grupo pendiente `SPECIALIZED` [`RN-COL-001`] y, solo si no hay ninguna alcanzable, evalúa supletoriamente el grupo `BASIC` [`RN-COL-002`]; al liberarse unidad `BASIC`: evalúa exclusivamente el grupo `BASIC` [`RN-COL-007`]; nunca constituye prioridad clínica [`RN-COL-004`]), **(b)** orden FIFO estricto dentro del grupo evaluado (`queue_entered_at_t ASC, id ASC`, sin *aging*, `RN-COL-003`, `RN-COL-005`) y **(c)** confirmación bajo `SELECT ... FOR UPDATE` con contador nuevo (`1+3`, `DEC-017`, `RN-CON-009`). | Vincula la unidad (`assigned_emergency_id = emergency.id`, `assigned_ambulance_id`), ejecuta `T-AMB-01` (`AVAILABLE -> ASSIGNED`) atómicamente, agenda arribo al incidente y sella `assigned_at_t = t`. |
| **`T-EME-04`** | `ASSIGNED` | `AT_INCIDENT` | `confirmarLlegadaYRecogidaEnIncidente()` (`HU-09`, `HU-13`) | Arribo de la ambulancia asignada al nodo del incidente en el tiempo lógico $t$. | Ejecuta `T-AMB-02` (`ASSIGNED -> AT_INCIDENT`), actualiza nodo de la ambulancia al nodo del incidente, sella `at_incident_at_t = t` y dispara la evaluación de reserva hospitalaria. |
| **`T-EME-05`** | `AT_INCIDENT` | `IN_TRANSPORT` | `seleccionarYReservarHospital()` (`HU-09`) | Hospital habilitado (`enabled = true`), alcanzable por Dijkstra y con `D = C - R - O >= 1` confirmado bajo `SELECT ... FOR UPDATE` dentro de `1..3` intentos (`1+2`). | Incrementa `R = R + 1` (`D = D - 1`), crea reserva (`RESERVED`), vincula `reserved_hospital_id`, ejecuta `T-AMB-03` (`AT_INCIDENT -> TRANSPORTING`), agenda arribo al hospital y sella `in_transport_at_t = t`. |
| **`T-EME-06`** | `AT_INCIDENT` | `WAITING_HOSPITAL` | `seleccionarYReservarHospital()` (`HU-09`) | Sin hospital elegible en el intento inicial (`0` reintentos) **o** agotamiento de los `3` intentos (`1+2`) por conflicto concurrente (`HOSPITAL_RESERVATION_RETRY_EXHAUSTED`). | Sella `hospital_queue_entered_at_t = t`, registra el motivo funcional y **retiene la ambulancia ocupada en `AT_INCIDENT`** sin iniciar traslado (`CA-SIM-03`). |
| **`T-EME-07`** | `WAITING_HOSPITAL` | `IN_TRANSPORT` | `reevaluarColaEsperaHospitalaria()` (`HU-10`, `HU-12`) | Liberación de capacidad (`D >= 1`) + evaluación FIFO (`hospital_queue_entered_at_t ASC, id ASC`) sin pérdida de posición + reserva confirmada bajo `SELECT ... FOR UPDATE` con contador nuevo (`1+2`). | Incrementa `R = R + 1` (`D = D - 1`), vincula `reserved_hospital_id`, ejecuta `T-AMB-03` (`AT_INCIDENT -> TRANSPORTING`), agenda arribo al hospital y sella `in_transport_at_t = t`. |
| **`T-EME-08`** | `IN_TRANSPORT` | `COMPLETED` | `confirmarLlegadaYEntregaEnHospital()` (`HU-11`, `HU-13`) | Arribo de la ambulancia al nodo del hospital reservado y confirmación de entrega simulada en el tiempo lógico $t$. | Transforma reserva en ocupación (`R = R - 1`, `O = O + 1`, `D` sin cambios), ejecuta `T-AMB-04` (`TRANSPORTING -> AVAILABLE` en el nodo del hospital), sella `completed_at_t = t` y dispara evaluación de `WAITING_AMBULANCE`. |
| **`T-EME-09`** | `COMPLETED` | `[*]` | Cierre del ciclo (`RN-EME-001`) | Estado final alcanzado. | Ninguna transición posterior sobre la emergencia (el alta simulada posterior solo libera `O = O - 1`, `D = D + 1` en el hospital). |

### 2.2 Transiciones de la Máquina de Estados de Ambulancia (`ambulance-states.puml`)

| ID | Estado origen | Estado destino | Evento desencadenante | Condición (Guarda) | Acción atómica y sincronización con Emergencia |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`T-AMB-00`** | `[*]` | `AVAILABLE` | `registrarAmbulanciaEnCatalogo()` / semilla inicial (`HU-03`) | Nodo válido (`N01`–`N10`) y equipamiento `BASIC` o `SPECIALIZED`. | Inicializa en `AVAILABLE` con `assigned_emergency_id = NULL`. |
| **`T-AMB-01`** | `AVAILABLE` | `ASSIGNED` | `confirmarAsignacion(emergencyId)` (`HU-06`, `HU-07`, `HU-08`) | `status == 'AVAILABLE'`, `assigned_emergency_id IS NULL`, equipamiento compatible (`RN-PRE-001..003`) y ruta alcanzable, validado bajo `SELECT ... FOR UPDATE`. | Asigna `assigned_emergency_id = emergencyId` en la misma transacción corta que `T-EME-01` o `T-EME-03` (`ASSIGNED`). |
| **`T-AMB-02`** | `ASSIGNED` | `AT_INCIDENT` | `confirmarLlegadaAlIncidente()` (`HU-09`, `HU-13`) | Arribo al nodo del incidente en tiempo lógico $t$. | Actualiza `current_node_id` al nodo del incidente en la misma transacción que `T-EME-04` (`AT_INCIDENT`). Si luego ocurre `T-EME-06` (`WAITING_HOSPITAL`), la ambulancia **se mantiene en `AT_INCIDENT`**. |
| **`T-AMB-03`** | `AT_INCIDENT` | `TRANSPORTING` | `confirmarReservaHospitalaria(hospitalId)` (`HU-09`, `HU-10`) | Confirmación atómica de reserva de plaza (`R = R + 1`) en hospital elegible bajo `SELECT ... FOR UPDATE`. | Inicia traslado hacia el nodo del hospital en la misma transacción corta que `T-EME-05` o `T-EME-07` (`IN_TRANSPORT`). |
| **`T-AMB-04`** | `TRANSPORTING` | `AVAILABLE` | `confirmarEntregaSimuladaEnHospital()` (`HU-11`, `HU-13`) | Arribo al nodo del hospital reservado y entrega simulada en tiempo lógico $t$. | Actualiza `current_node_id` al nodo del hospital, limpia `assigned_emergency_id = NULL` en la misma transacción que `T-EME-08` (`COMPLETED`) y dispara la evaluación de `WAITING_AMBULANCE` (`RN-RUT-008`). |

---

## 3. Correspondencia con `DEC-019`, reglas `RN-*` y criterios `CA-*`

| Aspecto del modelo de estados | Decisión | Reglas de negocio (`business-rules.md`) | Criterios de aceptación (`business-rules.md`) | Historias de usuario |
| :--- | :---: | :--- | :--- | :--- |
| **Catálogo cerrado de 7 estados de emergencia y 4 de ambulancia** | `DEC-019` | `RN-RUT-007` | `CA-SIM-03` | `HU-01`, `HU-02`, `HU-03`, `HU-06` a `HU-11` |
| **Asignación concurrente, revalidación y reintentos (`1+3`)** | `DEC-011`, `DEC-017`, `DEC-021` | `RN-ASI-001` a `RN-ASI-005`, `RN-CON-001` a `RN-CON-009` | `CA-CON-01` a `CA-CON-10`, `CA-SIM-02` | `HU-05`, `HU-06`, `HU-07` |
| **Cola FIFO `WAITING_AMBULANCE` por equipamiento (`SPECIALIZED` / `BASIC`)** | `DEC-012`, `DEC-019` | `RN-COL-001` a `RN-COL-007` | `CA-SIM-01` | `HU-08` |
| **Selección de hospital post-recogida y reserva concurrente (`1+2`)** | `DEC-014`, `DEC-018`, `DEC-021` | `RN-ELE-004`, `RN-HOS-002`, `RN-HOS-004` a `RN-HOS-009` | `CA-HOS-01` a `CA-HOS-07` | `HU-05`, `HU-09` |
| **Retención de ambulancia en `AT_INCIDENT` durante `WAITING_HOSPITAL` y FIFO sin pérdida de posición** | `DEC-018`, `DEC-019` | `RN-HOS-009` a `RN-HOS-011`, `RN-RUT-007` | `CA-HOS-08`, `CA-HOS-09`, `CA-SIM-03` | `HU-09`, `HU-10` |
| **Entrega en hospital (`COMPLETED`, `R -> O`), liberación inmediata (`TRANSPORTING -> AVAILABLE`) e independencia del alta simulada** | `DEC-007`, `DEC-018`, `DEC-019` | `RN-EME-001`, `RN-RUT-005`, `RN-RUT-008`, `RN-HOS-003`, `RN-HOS-012` a `RN-HOS-014` | `CA-HOS-10`, `CA-HOS-11`, `CA-HOS-13`, `CA-SIM-04` | `HU-11`, `HU-12` |
| **Avance manual por eventos discretos, reloj lógico monotónico ($t \ge 0$) y precedencia ante empates** | `DEC-019`, `DEC-021` | `RN-RUT-009`, `RN-MET-003`, `RN-MET-004` | `CA-SIM-06` a `CA-SIM-10` | `HU-13`, `HU-14` |

---

## 4. Invariantes críticas para implementación y pruebas automatizadas (`INV-SM-01` a `INV-SM-08`)

1. **`INV-SM-01` — Clausura de los conjuntos de estados (`DEC-019`, `RN-RUT-007`, `CA-SIM-03`):**
   - Toda emergencia pertenece en todo momento exclusivamente a uno de los **7 estados aprobados**: `{REGISTERED, WAITING_AMBULANCE, ASSIGNED, AT_INCIDENT, WAITING_HOSPITAL, IN_TRANSPORT, COMPLETED}`.
   - Toda ambulancia pertenece en todo momento exclusivamente a uno de los **4 estados aprobados**: `{AVAILABLE, ASSIGNED, AT_INCIDENT, TRANSPORTING}`.
   - Ninguna operación persiste estados fuera de estos dos conjuntos cerrados.
2. **`INV-SM-02` — Exclusividad de asociación activa mediante `assigned_emergency_id` (`RN-CON-001`, `CA-CON-07`, `ADR-0002` Secc. 4.1):**
   - La referencia `ambulances.assigned_emergency_id` (`UNIQUE` en base de datos, anulable) vincula a una ambulancia con **a lo sumo una** emergencia activa.
   - Para toda ambulancia $a$:
     - $a.\text{state} = \texttt{AVAILABLE} \iff a.\text{assigned\_emergency\_id} = \texttt{NULL}$
     - $a.\text{state} \in \{\texttt{ASSIGNED}, \texttt{AT\_INCIDENT}, \texttt{TRANSPORTING}\} \iff a.\text{assigned\_emergency\_id} \neq \texttt{NULL}$
   - Nunca pueden existir dos ambulancias con el mismo `assigned_emergency_id` no nulo, ni una ambulancia asignada simultáneamente a dos emergencias activas.
3. **`INV-SM-03` — Acoplamiento de estados acotado exclusivamente al par emergencia–ambulancia activamente vinculado (`DEC-019`, `RN-RUT-007`, `CA-SIM-03`):**
   - Las equivalencias entre estados de emergencia y de ambulancia se aplican **exclusivamente a una emergencia $e$ y a su ambulancia $a$ activamente vinculada mediante `a.assigned_emergency_id == e.id`** (nunca como restricción global sobre el conjunto total de ambulancias del sistema):
     - Si $e.\text{state} \in \{\texttt{REGISTERED}, \texttt{WAITING\_AMBULANCE}, \texttt{COMPLETED}\}$, entonces **ninguna** ambulancia $a$ mantiene un vínculo activo con $e$ ($\neg \exists a : a.\text{assigned\_emergency\_id} = e.\text{id}$). En `COMPLETED`, la ambulancia que realizó el traslado ya fue desvinculada (`a.assigned_emergency_id = NULL` y `a.state = AVAILABLE` tras `T-AMB-04`), sin perjuicio de que el registro experimental conserve la referencia histórica del traslado (`RN-MET-003`).
     - Para todo par $(e, a)$ activamente vinculado tal que `a.assigned_emergency_id == e.id`:
       - $e.\text{state} = \texttt{ASSIGNED} \iff a.\text{state} = \texttt{ASSIGNED}$
       - $e.\text{state} \in \{\texttt{AT\_INCIDENT}, \texttt{WAITING\_HOSPITAL}\} \iff a.\text{state} = \texttt{AT\_INCIDENT}$
       - $e.\text{state} = \texttt{IN\_TRANSPORT} \iff a.\text{state} = \texttt{TRANSPORTING}$
4. **`INV-SM-04` — Conservación e integridad de capacidad hospitalaria (`DEC-018`, `RN-HOS-004`, `RN-HOS-007`, `RN-HOS-012`, `CA-HOS-04`, `CA-HOS-10`):**
   - En todo hospital se cumple en cualquier instante $C \ge 0$, $R \ge 0$, $O \ge 0$, $R + O \le C$ y $D = C - R - O \ge 0$.
   - Toda emergencia en `IN_TRANSPORT` posee exactamente una reserva hospitalaria activa (`status = 'RESERVED'`); al confirmarse `T-EME-08` (`IN_TRANSPORT -> COMPLETED`), dicha reserva se convierte atómicamente en ocupación (`R = R - 1`, `O = O + 1`, manteniendo `D` sin cambios).
5. **`INV-SM-05` — Separación entre ingreso inmediato a cola y reintentos concurrentes acotados (`DEC-017`, `DEC-018`, `RN-CON-005` a `RN-CON-009`, `RN-HOS-008`):**
   - Ante `0` candidatos elegibles en el intento inicial, la emergencia transita a `WAITING_AMBULANCE` (`T-EME-02`) o `WAITING_HOSPITAL` (`T-EME-06`) consumiendo `0` reintentos por concurrencia.
   - Los reintentos automáticos se activan únicamente ante conflictos concurrentes reintentables (`ADR-0002` Secc. 4.4), con límites independientes de `1 + 3` intentos en asignación automática de ambulancia (`CONCURRENCY_RETRY_EXHAUSTED`) y `1 + 2` intentos en reserva hospitalaria (`HOSPITAL_RESERVATION_RETRY_EXHAUSTED`). Cada nueva operación independiente disparada desde cola reinicia su contador desde cero.
6. **`INV-SM-06` — Distinción entre compatibilidad, preservación y orden FIFO sin *aging* ni pérdida de posición (`DEC-012`, `DEC-018`, `DEC-019`, `RN-COL-001` a `RN-COL-007`, `RN-PRE-001` a `RN-PRE-003`, `RN-HOS-010`, `RN-HOS-011`, `CA-SIM-01`, `CA-HOS-09`):**
   - En `WAITING_AMBULANCE` (`T-EME-03`) se distinguen tres reglas ortogonales sin introducir prioridad clínica (`RN-COL-004`, `RN-EME-008`):
     1. **Compatibilidad de equipamiento:** una unidad `BASIC` solo es compatible con emergencias `BASIC`; una unidad `SPECIALIZED` es compatible con `SPECIALIZED` y `BASIC` (`RN-EME-007`, `RN-PRE-003`).
     2. **Preservación de ambulancias especializadas:** al liberarse una unidad `SPECIALIZED`, se evalúa primero el grupo pendiente `SPECIALIZED` (`RN-COL-001`) y, únicamente si no existe ninguna emergencia alcanzable en ese grupo, se evalúa de manera supletoria el grupo `BASIC` (`RN-COL-002`); al liberarse una unidad `BASIC`, se evalúa exclusivamente el grupo `BASIC` (`RN-COL-007`, `CA-SIM-01`); al seleccionar unidad para una emergencia `BASIC`, se prefieren unidades `BASIC` sobre `SPECIALIZED` (`RN-PRE-001`, `RN-PRE-002`).
     3. **Orden FIFO estricto:** dentro de cada grupo evaluado se respeta estrictamente el orden FIFO (`queue_entered_at_t ASC, id ASC`, `RN-COL-003`) sin *aging* (`RN-COL-005`).
   - En `WAITING_HOSPITAL` (`T-EME-07`), la reevaluación sigue estricto orden FIFO (`hospital_queue_entered_at_t ASC, id ASC`, `RN-HOS-010`); si el hospital liberado es inalcanzable para la primera emergencia en cola, esta conserva intacta su marca `hospital_queue_entered_at_t` y su posición original mientras se evalúa la siguiente (`RN-HOS-011`, `CA-HOS-09`).
7. **`INV-SM-07` — Independencia entre liberación de ambulancia y alta hospitalaria simulada (`DEC-018`, `DEC-019`, `RN-RUT-008`, `RN-HOS-013`, `RN-HOS-014`, `CA-SIM-04`, `CA-HOS-11`, `CA-HOS-13`):**
   - La transición `T-AMB-04` (`TRANSPORTING -> AVAILABLE`, `assigned_emergency_id = NULL`) ocurre siempre y únicamente junto con `T-EME-08` (`IN_TRANSPORT -> COMPLETED`) en el instante de la entrega simulada en el hospital (`R = R - 1`, `O = O + 1`).
   - El evento posterior de alta simulada (`HU-12`, `O = O - 1`, `D = D + 1`, idempotente sobre la ocupación `OCCUPIED`) jamás modifica el estado de una ambulancia ni reabre una emergencia `COMPLETED`.
8. **`INV-SM-08` — Atomicidad transaccional, precedencia de eventos discretos y orden canónico relativo de bloqueos (`DEC-019`, `DEC-021` / `ADR-0002` Secc. 4.3, 4.7 y 4.9, `RN-RUT-009`, `CA-SIM-07`):**
   - Cada intento de transición acoplada (`T-EME-01`/`T-AMB-01`, `T-EME-03`/`T-AMB-01`, `T-EME-04`/`T-AMB-02`, `T-EME-05`/`T-AMB-03`, `T-EME-07`/`T-AMB-03` y `T-EME-08`/`T-AMB-04`) se confirma o revierte dentro de una **transacción corta** con `@Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED, timeout = 5)`.
   - El orden canónico global de adquisición de bloqueos pesimistas (`SELECT ... FOR UPDATE`) establecido por `ADR-0002` (Sección 4, punto 7) es:
     $$\texttt{Scenario}\ (\texttt{simulation\_scenarios}) \longrightarrow \texttt{Emergency}\ (\texttt{emergencies},\ \texttt{id ASC}) \longrightarrow \texttt{Ambulance}\ (\texttt{ambulances},\ \texttt{id ASC}) \longrightarrow \texttt{Hospital}\ (\texttt{hospitals},\ \texttt{id ASC})$$
   - Cada operación bloquea **únicamente las entidades necesarias** para su transición específica (por ejemplo, `Emergency` $\to$ `Ambulance` en asignación o llegada al incidente; `Emergency` $\to$ `Ambulance` $\to$ `Hospital` en reserva o entrega hospitalaria; `Scenario` al inicio solo cuando la operación requiere bloquear el escenario), conservando siempre el **orden relativo aprobado** `Scenario → Emergency → Ambulance → Hospital`.
   - Ante empate de tiempo lógico ($t_a = t_b$) en el planificador de eventos discretos (`RN-RUT-009`, `CA-SIM-07`), se ejecuta el orden fijo de precedencia: (1.°) liberación de recursos (`T-EME-08`/`T-AMB-04` y alta simulada), (2.°) llegada al incidente (`T-EME-04`/`T-AMB-02`), (3.°) evaluación de colas en espera (`T-EME-03`, `T-EME-07`) y (4.°) registro de nueva emergencia (`T-EME-00`), desempatando dentro del mismo tipo por secuencia monotónica creciente / identificador técnico estable.

---

## 5. Verificación de ambigüedades o transiciones pendientes de aclaración

- **Transiciones del ciclo operativo (`DEC-019`):** no existen transiciones ambiguas ni sin definir entre los 7 estados de emergencia ni entre los 4 estados de ambulancia; todas las condiciones de guarda, límites de reintentos (`1+3` en ambulancia y `1+2` en hospital) y efectos laterales sobre las colas FIFO están cerrados en `DEC-016` a `DEC-021`.
- **Único aspecto administrativo diferido fuera de las máquinas de estados (`stakeholders.md` Secc. 5.2):** mientras que la publicación de reglas (`RN-EME-012`) y la edición de la capacidad hospitalaria `C` (`RN-HOS-015`) están expresamente bloqueadas durante escenarios activos, permanece diferido en `stakeholders.md` (Sección 5.2) si se permitirá editar el catálogo administrativo de `ambulancias` durante un escenario activo o únicamente antes/después de cada ejecución. Esto no agrega estados ni altera las transiciones `T-AMB-00` a `T-AMB-04`.

# Especificación del Diagrama UML de Casos de Uso del MVP — ARE (Arequipa RescUE)

**Estado del documento:** Aprobado — Modelo UML de casos de uso del MVP (`CU-01` a `CU-15`) alineado con el backlog aprobado (`HU-01` a `HU-14`, commit `0927b6c`) y las decisiones `DEC-001` a `DEC-021`
**Archivo PlantUML asociado:** [`are-use-cases.puml`](file:///c:/Users/giovani/Documents/GitHub/ProyectoFinalTO-Arequipa-RescUE/docs/architecture/uml/are-use-cases.puml)
**Propósito:** Documentar el modelo funcional de casos de uso del MVP académico de **ARE – Arequipa RescUE**, estableciendo la correspondencia exacta entre los tres actores humanos aprobados (`DEC-009`), los **15 casos de uso identificados (`CU-01` a `CU-15`)** y las **14 historias de usuario aprobadas (`HU-01` a `HU-14`)**.

---

## 1. Actores del sistema (`DEC-009`)

Conforme a `DEC-009` y [`stakeholders.md`](file:///c:/Users/giovani/Documents/GitHub/ProyectoFinalTO-Arequipa-RescUE/docs/product/stakeholders.md), el diagrama utiliza **exclusivamente los tres actores humanos aprobados**. El **Motor de simulación (`CMP-INT-01`)** es un componente interno del límite del sistema ARE y no se representa como actor externo:

| ID | Actor humano | Rol funcional frente al límite del sistema ARE | Casos de uso asociados |
| :--- | :--- | :--- | :--- |
| **`ACT-01`** | **Operador de recepción** | Registra solicitudes de emergencia entrantes para su clasificación simulada determinista, hace seguimiento del estado de las emergencias (`REGISTERED` a `COMPLETED`) y consulta en modo lectura la versión vigente de reglas de clasificación. | `CU-01`, `CU-02`, `CU-03` |
| **`ACT-02`** | **Despachador** | Consulta emergencias y la versión vigente de reglas en modo lectura, evalúa ambulancias elegibles, ejecuta asignaciones supervisadas o supervisa asignaciones automáticas (incluyendo avisos por agotamiento de reintentos), gestiona las colas FIFO (`WAITING_AMBULANCE` y `WAITING_HOSPITAL`) y confirma llegada al incidente, reserva hospitalaria y entrega simulada en el hospital. | `CU-02`, `CU-03`, `CU-04`, `CU-05`, `CU-06`, `CU-07`, `CU-08`, `CU-09`, `CU-10` |
| **`ACT-03`** | **Administrador** | Administra los catálogos de ambulancias, hospitales y el grafo vial de 10 nodos (`N01` a `N10`), publica y activa versiones inmutables de reglas de clasificación (además de consultarlas), ejecuta el alta simulada hospitalaria, conduce escenarios experimentales con avance manual por eventos discretos y consulta las 7 métricas y el registro experimental. | `CU-03`, `CU-11`, `CU-12`, `CU-13`, `CU-14`, `CU-15` |

---

## 2. Casos de uso identificados por agrupación funcional (`CU-01` a `CU-15`)

El límite del sistema **`ARE — Arequipa RescUE (Simulador Académico MVP)`** organiza sus funcionalidades en cinco paquetes visuales cohesivos, sin exponer algoritmos internos (como Dijkstra), tablas SQL ni componentes de Spring Boot como casos de uso:

### 2.1 Paquete 1: Gestión de emergencias

| ID | Caso de uso | Actor(es) | Descripción funcional sintética | Estados involucrados (`DEC-019`) |
| :--- | :--- | :---: | :--- | :--- |
| **`CU-01`** | **Registrar y clasificar solicitud de emergencia** | `ACT-01` | Registra una solicitud ingresando categoría (`INC-A`..`INC-D`), nodo (`N01`..`N10`) y descripción opcional; el sistema determina de forma reproducible su prioridad informativa y equipamiento requerido (`BASIC`/`SPECIALIZED`) asociando la versión activa de reglas. Si no hay ambulancias elegibles al evaluar, pasa a espera. | Emergencia: `REGISTERED` *(o `WAITING_AMBULANCE`)* |
| **`CU-02`** | **Consultar y dar seguimiento a emergencias** | `ACT-01`, `ACT-02` | Muestra el listado y detalle de las emergencias registradas, su clasificación, versión de reglas aplicada, recursos asignados (ambulancia y hospital) y marcas de tiempo lógico $t$ a lo largo de sus estados aprobados. | Emergencia: `REGISTERED` a `COMPLETED` |
| **`CU-03`** | **Consultar versión vigente de reglas de clasificación** | `ACT-01`, `ACT-02`, `ACT-03` | Permite visualizar en modo de solo lectura la versión activa del catálogo de reglas (`RUL-VXX`) y la correspondencia de cada categoría (`INC-A` a `INC-D`) con su prioridad informativa y equipamiento requerido. | — |

### 2.2 Paquete 2: Gestión de ambulancias y despacho

| ID | Caso de uso | Actor(es) | Descripción funcional sintética | Estados involucrados (`DEC-019`) |
| :--- | :--- | :---: | :--- | :--- |
| **`CU-04`** | **Consultar ambulancias elegibles para emergencia** | `ACT-02` | Determina y presenta las ambulancias en estado `AVAILABLE`, alcanzables en el grafo y compatibles en equipamiento (`BASIC`/`SPECIALIZED`), ordenadas por preservación de especializadas, menor costo de ruta y desempate determinista por ID estable (`RN-ASI-005`), con independencia de la capacidad hospitalaria. | Emergencia: `REGISTERED` / `WAITING_AMBULANCE`; Ambulancia: `AVAILABLE` |
| **`CU-05`** | **Asignar ambulancia en modalidad supervisada** | `ACT-02` | Permite al `Despachador` elegir manualmente cualquier ambulancia elegible para asignarla; si otra operación concurrente tomó la unidad antes de confirmar, rechaza la confirmación sin reintentos automáticos, refresca las elegibles y solicita nueva selección manual. | Emergencia: `ASSIGNED` *(o `WAITING_AMBULANCE`)*; Ambulancia: `AVAILABLE -> ASSIGNED` |
| **`CU-06`** | **Ejecutar y supervisar asignación automática de ambulancia** | `ACT-02` | Selecciona y asigna automáticamente la ambulancia óptima con hasta `1` intento inicial y `3` reintentos adicionales ante conflictos concurrentes; si agota los 4 intentos, conserva la emergencia en `WAITING_AMBULANCE`, registra `CONCURRENCY_RETRY_EXHAUSTED` y muestra aviso visible al `Despachador`. | Emergencia: `ASSIGNED` *(o `WAITING_AMBULANCE`)*; Ambulancia: `AVAILABLE -> ASSIGNED` |
| **`CU-07`** | **Gestionar cola de espera de ambulancia (`WAITING_AMBULANCE`)** | `ACT-02` | Permite visualizar la cola de emergencias en `WAITING_AMBULANCE` y ejecuta su atención FIFO sin *aging* por grupo de equipamiento (`SPECIALIZED` primero y luego `BASIC` para unidades especializadas; solo `BASIC` para unidades básicas) con contador nuevo de intentos por operación. | Emergencia: `WAITING_AMBULANCE -> ASSIGNED`; Ambulancia: `AVAILABLE -> ASSIGNED` |

### 2.3 Paquete 3: Gestión hospitalaria

| ID | Caso de uso | Actor(es) | Descripción funcional sintética | Estados involucrados (`DEC-019`) |
| :--- | :--- | :---: | :--- | :--- |
| **`CU-08`** | **Confirmar llegada al incidente y reservar plaza hospitalaria** | `ACT-02` | Confirma el arribo y recogida en el nodo del incidente (`AT_INCIDENT`) y selecciona el hospital habilitado, alcanzable y con plaza disponible (`D = C - R - O >= 1`, sin especialidades clínicas) de menor costo y desempate por ID, ejecutando la reserva (`R = R + 1`) con hasta `1+2` intentos concurrentes. Si no hay hospital elegible o se agotan intentos (`HOSPITAL_RESERVATION_RETRY_EXHAUSTED`), pasa a `WAITING_HOSPITAL` reteniendo la ambulancia en `AT_INCIDENT`. | Emergencia: `ASSIGNED -> AT_INCIDENT -> IN_TRANSPORT` *(o `WAITING_HOSPITAL`)*; Ambulancia: `ASSIGNED -> AT_INCIDENT -> TRANSPORTING` *(o permanece en `AT_INCIDENT`)* |
| **`CU-09`** | **Gestionar cola de espera de destino hospitalario (`WAITING_HOSPITAL`)** | `ACT-02` | Permite supervisar la cola FIFO de emergencias en `WAITING_HOSPITAL` y su reevaluación automática con contador nuevo de intentos cuando se libera capacidad (`D >= 1`), conservando la posición original de aquellas emergencias para las que el hospital liberado sea inalcanzable. | Emergencia: `WAITING_HOSPITAL -> IN_TRANSPORT`; Ambulancia: `AT_INCIDENT -> TRANSPORTING` |
| **`CU-10`** | **Confirmar entrega simulada en hospital y liberar ambulancia** | `ACT-02` | Registra la llegada de la ambulancia al nodo del hospital reservado y la entrega simulada, cerrando la emergencia en `COMPLETED`, transformando la reserva en ocupación (`R = R - 1`, `O = O + 1`) y pasando inmediatamente la ambulancia a `AVAILABLE` sin esperar al alta hospitalaria. | Emergencia: `IN_TRANSPORT -> COMPLETED`; Ambulancia: `TRANSPORTING -> AVAILABLE` |
| **`CU-11`** | **Ejecutar alta simulada hospitalaria** | `ACT-03` | Permite al `Administrador` liberar de forma explícita e idempotente una plaza hospitalaria ocupada (`O = O - 1`, `D = D + 1`), rechazando ejecuciones duplicadas sobre la misma ocupación y sin alterar el estado de ninguna ambulancia. | Emergencia: permanece `COMPLETED`; Ambulancia: permanece independiente |

### 2.4 Paquete 4: Administración y simulación

| ID | Caso de uso | Actor(es) | Descripción funcional sintética | Estados involucrados (`DEC-019`) |
| :--- | :--- | :---: | :--- | :--- |
| **`CU-12`** | **Administrar catálogo de ambulancias, hospitales y grafo vial (`N01`..`N10`)** | `ACT-03` | Permite al `Administrador` registrar, actualizar y consultar ambulancias (`BASIC`/`SPECIALIZED`), hospitales (`C`, `R`, `O`, `D`) y la topología inicial de 10 nodos (`N01`–`N09` conectados y `N10` aislado), validando `R + O <= C` y bloqueando cambios de `C` durante escenarios activos. | Ambulancia: `AVAILABLE` (configuración base) |
| **`CU-13`** | **Publicar y activar versión inmutable de reglas de clasificación** | `ACT-03` | Permite al `Administrador` crear, publicar y activar una nueva versión inmutable del catálogo de reglas (`INC-A` a `INC-D`) cuando no hay un escenario experimental activo, sin recalcular emergencias históricas. | — |
| **`CU-14`** | **Ejecutar escenario experimental y avanzar eventos discretos manualmente** | `ACT-03` | Permite al `Administrador` iniciar un escenario experimental y accionar manualmente el paso al siguiente evento discreto desde el panel de simulación, actualizando el tiempo lógico monotónico ($t \ge 0$), aplicando la precedencia determinista ante empates temporales y bloqueando cambios estructurales mientras el escenario está activo. | Coordina transiciones válidas de `DEC-019` según el tipo de evento |

### 2.5 Paquete 5: Métricas y reportes

| ID | Caso de uso | Actor(es) | Descripción funcional sintética | Estados involucrados (`DEC-019`) |
| :--- | :--- | :---: | :--- | :--- |
| **`CU-15`** | **Consultar catálogo de 7 métricas y registro experimental trazable** | `ACT-03` | Presenta al `Administrador` las 7 métricas experimentales aprobadas (`RN-MET-004`, diferenciando operaciones completadas y en curso para observar *starvation*) y el registro funcional trazable por emergencia (`RN-MET-003`, con conteo desglosado de conflictos y agotamientos `DEC-017`/`DEC-018`). | Consulta histórica y en curso (`REGISTERED` a `COMPLETED`) |

---

## 3. Matriz de trazabilidad: Casos de Uso (`CU-01` a `CU-15`) $\leftrightarrow$ Historias de Usuario (`HU-01` a `HU-14`)

Siguiendo el criterio de diseño UML, la relación es **1 a 1** en 13 de las 14 historias, y **2 a 1** únicamente en **`HU-04`**, la cual se divide en **`CU-13`** (publicación y activación inmutable exclusiva del `Administrador` `ACT-03`) y **`CU-03`** (consulta en modo lectura de la versión vigente para `ACT-01`, `ACT-02` y `ACT-03`), evitando atribuir permisos de escritura sobre reglas a recepción o despacho en el diagrama UML:

| Caso de uso | Título del caso de uso | Historia(s) de usuario cubierta(s) | Actor(es) en UML | Relaciones UML en el diagrama | Reglas y criterios principales |
| :--- | :--- | :---: | :---: | :--- | :--- |
| **`CU-01`** | Registrar y clasificar solicitud de emergencia | **`HU-01`** | `ACT-01` | — | `RN-EME-001` a `RN-EME-010`, `RN-ASI-004`, `CA-SIM-03` |
| **`CU-02`** | Consultar y dar seguimiento a emergencias | **`HU-02`** | `ACT-01`, `ACT-02` | — | `RN-EME-001`, `RN-EME-004`, `RN-EME-006`, `RN-RUT-005`, `RN-RUT-007`, `CA-SIM-03` |
| **`CU-03`** | Consultar versión vigente de reglas de clasificación | **`HU-04`** *(modo lectura)* | `ACT-01`, `ACT-02`, `ACT-03` | — | `RN-EME-002` a `RN-EME-008`, `RN-EME-011`, `DEC-009`, `DEC-016` |
| **`CU-04`** | Consultar ambulancias elegibles para emergencia | **`HU-05`** | `ACT-02` | Incluido por `CU-05` y `CU-06` (`<<include>>`) | `RN-ELE-001` a `RN-ELE-004`, `RN-ASI-005`, `RN-PRE-001` a `RN-PRE-003`, `RN-RUT-001` a `RN-RUT-006`, `CA-SIM-02`, `CA-SIM-05` |
| **`CU-05`** | Asignar ambulancia en modalidad supervisada | **`HU-06`** | `ACT-02` | `<<include>> CU-04` (`RN-ELE-001`) | `RN-ASI-001`, `RN-ASI-002`, `RN-CON-001`, `RN-CON-002`, `RN-CON-005`, `CA-CON-07`, `CA-CON-10` |
| **`CU-06`** | Ejecutar y supervisar asignación automática de ambulancia | **`HU-07`** | `ACT-02` | `<<include>> CU-04` (`RN-ELE-001`, `RN-CON-006`) | `RN-ASI-001`, `RN-ASI-003` a `RN-ASI-005`, `RN-CON-001`, `RN-CON-003` a `RN-CON-009`, `CA-CON-01` a `CA-CON-09` |
| **`CU-07`** | Gestionar cola de espera de ambulancia (`WAITING_AMBULANCE`) | **`HU-08`** | `ACT-02` | — *(independiente)* | `RN-COL-001` a `RN-COL-007`, `RN-CON-009`, `RN-RUT-008`, `CA-SIM-01` |
| **`CU-08`** | Confirmar llegada al incidente y reservar plaza hospitalaria | **`HU-09`** | `ACT-02` | — | `RN-HOS-001`, `RN-HOS-004` a `RN-HOS-009`, `CA-HOS-01` a `CA-HOS-07`, `CA-SIM-03` |
| **`CU-09`** | Gestionar cola de espera de destino hospitalario (`WAITING_HOSPITAL`) | **`HU-10`** | `ACT-02` | — *(independiente)* | `RN-HOS-005` a `RN-HOS-011`, `CA-HOS-08`, `CA-HOS-09`, `CA-SIM-03` |
| **`CU-10`** | Confirmar entrega simulada en hospital y liberar ambulancia | **`HU-11`** | `ACT-02` | — *(independiente)* | `RN-EME-001`, `RN-RUT-005`, `RN-RUT-008`, `RN-HOS-003`, `RN-HOS-012`, `RN-HOS-014`, `CA-HOS-10`, `CA-HOS-13`, `CA-SIM-04` |
| **`CU-11`** | Ejecutar alta simulada hospitalaria | **`HU-12`** | `ACT-03` | — *(independiente)* | `RN-HOS-011`, `RN-HOS-013`, `RN-HOS-014`, `CA-HOS-08`, `CA-HOS-11`, `CA-HOS-13` |
| **`CU-12`** | Administrar catálogo de ambulancias, hospitales y grafo vial (`N01`..`N10`) | **`HU-03`** | `ACT-03` | — | `RN-EME-007`, `RN-RUT-001`, `RN-RUT-006`, `RN-HOS-001`, `RN-HOS-004`, `RN-HOS-015`, `CA-HOS-12`, `CA-SIM-05` |
| **`CU-13`** | Publicar y activar versión inmutable de reglas de clasificación | **`HU-04`** *(gestión/publicación)* | `ACT-03` | — | `RN-EME-002` a `RN-EME-008`, `RN-EME-011`, `RN-EME-012`, `DEC-016` |
| **`CU-14`** | Ejecutar escenario experimental y avanzar eventos discretos manualmente | **`HU-13`** | `ACT-03` | — | `RN-EME-006`, `RN-EME-012`, `RN-RUT-009`, `RN-HOS-015`, `RN-MET-001`, `CA-HOS-12`, `CA-SIM-06`, `CA-SIM-07` |
| **`CU-15`** | Consultar catálogo de 7 métricas y registro experimental trazable | **`HU-14`** | `ACT-03` | — | `RN-EME-004`, `RN-CON-008`, `RN-COL-006`, `RN-HOS-008`, `RN-HOS-009`, `RN-MET-001` a `RN-MET-004`, `CA-SIM-08` a `CA-SIM-10` |

---

## 4. Justificación semántica de las relaciones UML y del comportamiento de reevaluación de colas

### 4.1 Relaciones `<<include>>` incluidas en el diagrama

El diagrama incluye únicamente **dos relaciones `<<include>>`**, respaldadas por las reglas de negocio aprobadas y la semántica de UML 2.5:

1. **`CU-05 (Asignar ambulancia en modalidad supervisada) ..> CU-04 (Consultar ambulancias elegibles para emergencia) : <<include>>`**
   - **Semántica UML y alcance funcional:** Representa que toda asignación supervisada incorpora de forma obligatoria la **evaluación funcional de ambulancias elegibles** (`CU-04`: disponibilidad, estado `AVAILABLE`, equipamiento `BASIC`/`SPECIALIZED`, preservación de unidades especializadas, cálculo de ruta mínima y desempate `RN-ASI-005`). No implica la obligación de abrir previamente una pantalla separada de consulta cuando la selección se realiza dentro del propio flujo de despacho (`RN-ELE-001`, `RN-ASI-002`).
2. **`CU-06 (Ejecutar y supervisar asignación automática de ambulancia) ..> CU-04 (Consultar ambulancias elegibles para emergencia) : <<include>>`**
   - **Semántica UML y alcance funcional:** Representa que toda operación de asignación automática —tanto en su intento inicial como en cada uno de sus hasta 3 reintentos ante conflictos concurrentes— ejecuta obligatoriamente la **evaluación funcional de ambulancias elegibles** (`CU-04` según `RN-ELE-001`, `RN-ASI-003` y `RN-CON-006`), sin requerir que el usuario abra una pantalla de consulta previa.

### 4.2 Independencia de los casos de uso de colas (`CU-07` y `CU-09`) frente a `CU-10` y `CU-11`

No se utilizan relaciones entre `CU-07` y `CU-10` ni entre `CU-09` y `CU-11`:

1. **Independencia entre `CU-07` (`WAITING_AMBULANCE`) y `CU-10` (Confirmar entrega simulada en hospital y liberar ambulancia):**
   - La gestión y supervisión de la cola de espera de ambulancia (`CU-07`, `HU-08`) es un caso de uso independiente de la confirmación de entrega simulada en el hospital (`CU-10`, `HU-11`).
   - Cuando la entrega simulada en el hospital (`CU-10`) libera una ambulancia (`TRANSPORTING -> AVAILABLE` según `RN-RUT-008`), la evaluación y atención FIFO de las emergencias pendientes en `WAITING_AMBULANCE` (`RN-COL-001` a `RN-COL-007`) ocurre como un **comportamiento interno del sistema** desencadenado por el cambio de disponibilidad del recurso, no como una extensión UML de casos de uso.
2. **Independencia entre `CU-09` (`WAITING_HOSPITAL`) y `CU-11` (Ejecutar alta simulada hospitalaria):**
   - La gestión y supervisión de la cola de espera de destino hospitalario (`CU-09`, `HU-10`) es un caso de uso independiente del evento de alta simulada hospitalaria (`CU-11`, `HU-12`).
   - Cuando el alta simulada (`CU-11`) libera una plaza ocupada (`O = O - 1`, `D = D + 1` según `RN-HOS-013`), la reevaluación automática FIFO sin pérdida de posición sobre `WAITING_HOSPITAL` (`RN-HOS-011`, `CA-HOS-08`, `CA-HOS-09`) se ejecuta como **comportamiento interno del sistema** ante el incremento de capacidad disponible, sin acoplar ambos casos de uso en el diagrama UML.

---

## 5. Observaciones relevantes para el futuro prototipado de interfaces (Google Stitch / Thymeleaf)

A partir de los 15 casos de uso y sus fronteras por actor, el futuro prototipo de interfaz web MVC (`Spring MVC + Thymeleaf`) podrá estructurarse de forma limpia en **tres consolas operativas con selector académico de rol** (sin pantalla de login por exclusión de autenticación en `DEC-008`):

1. **Consola de Recepción (`ACT-01` — `CU-01`, `CU-02`, `CU-03`):**
   - Formulario rápido de registro de emergencia con selector de categoría (`INC-A` a `INC-D`), selector de nodo del grafo (`N01` a `N10`) y campo de texto opcional para descripción ficticia (`CU-01`).
   - Tabla en vivo de seguimiento de emergencias con indicador visual de los **7 estados aprobados** (`REGISTERED`, `WAITING_AMBULANCE`, `ASSIGNED`, `AT_INCIDENT`, `WAITING_HOSPITAL`, `IN_TRANSPORT`, `COMPLETED`), mostrando la prioridad informativa, el equipamiento requerido y la versión de reglas aplicada (`CU-02`), más un modal/pestaña de solo lectura con la versión vigente de reglas (`CU-03`).
2. **Consola de Despacho y Operaciones (`ACT-02` — `CU-02` a `CU-10`):**
   - Panel dividido entre **Emergencias Activas / En Ruta** y las **dos colas FIFO diferenciadas visualmente**:
     - Cola `WAITING_AMBULANCE` (`CU-07`, indicando grupo `SPECIALIZED` / `BASIC` y tiempo lógico de ingreso).
     - Cola `WAITING_HOSPITAL` (`CU-09`, destacando que la emergencia retiene su ambulancia ocupada en `AT_INCIDENT`).
   - Modal o panel lateral de **Selección y Despacho** que liste las ambulancias elegibles ordenadas por preservación, costo Dijkstra y desempate `AMB-XX` (`CU-04`), permitiendo elegir cualquier unidad en modo supervisado (`CU-05`) o disparar/observar el despacho automático (`CU-06`).
   - **Banner/alerta visible de agotamiento de concurrencia** en la consola del `Despachador` cuando una asignación automática termine en `WAITING_AMBULANCE` con motivo `CONCURRENCY_RETRY_EXHAUSTED` tras los 4 intentos (`CU-06`, `CA-CON-06`).
   - Botones de transición operativa para confirmar llegada al incidente + reserva hospitalaria automática (`CU-08`) y confirmar entrega simulada en el hospital + liberación inmediata de ambulancia (`CU-10`).
3. **Consola de Administración, Simulación y Métricas (`ACT-03` — `CU-03`, `CU-11` a `CU-15`):**
   - **Panel de Catálogos y Grafo (`CU-12`, `CU-13`):** administración de ambulancias, hospitales (`C`, `R`, `O`, `D` con acción explícita de **Alta Simulada** `CU-11` sobre ocupaciones activas), visualización de los 10 nodos (`N01`–`N10`, incluyendo `N10` aislado) y publicación de versiones de reglas (`CU-13`), mostrando estado deshabilitado/bloqueado para la edición de `C` y la publicación de reglas cuando un escenario experimental está activo (`CA-HOS-12`, `RN-EME-012`).
   - **Panel de Control de Simulación por Eventos Discretos (`CU-14`):** selector de escenario experimental, indicador prominente del **reloj lógico actual ($t \ge 0$)**, cola del próximo evento discreto pendiente y botón de **Avanzar siguiente evento** paso a paso.
   - **Panel de Observabilidad y Métricas (`CU-15`):** tarjetas con las **7 métricas aprobadas** (`RN-MET-004`, separando promedios cerrados de indicadores en curso `EN_ESPERA` / `EN_ESPERA_HOSPITAL` para evidenciar *starvation*) y tabla del registro funcional trazable (`RN-MET-003`) con desglose de conflictos y reintentos (`DEC-017` y `DEC-018`).

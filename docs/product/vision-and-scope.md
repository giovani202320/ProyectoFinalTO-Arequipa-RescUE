# Visión y Alcance del Producto — ARE (Arequipa RescUE)

**Estado del documento:** Aprobado — Versión inicial aceptada (`DEC-015`), actualizada con el cierre funcional completo del MVP (`DEC-016` a `DEC-019`, cerrando `PEN-002a` a `PEN-002d`) y con la Entrega técnica 1 (`DEC-020` / `ADR-0001`, cerrando `PEN-003`, `PEN-004`, `PEN-005` y `PEN-007`; pendientes abiertos en `PEN-006` y `PEN-008`)
**Naturaleza del documento:** Documento de visión y alcance del producto en su versión inicial aceptada (`DEC-015`) y actualizada con `DEC-016` a `DEC-020`, que incorpora formalmente los acuerdos funcionales, de MVP y de base arquitectónica/técnica aprobados por el responsable del proyecto (`DEC-001` a `DEC-020`), distinguiéndolos de las decisiones técnicas y aspectos diferidos que continúan pendientes de definición (`PEN-006`, `PEN-008` y `stakeholders.md` Secc. 5).

---

## 1. Identificación del producto

- **Nombre del proyecto:** ARE — Arequipa RescUE.
- **Repositorio:** `ProyectoFinalTO-Arequipa-RescUE`.
- **Tipo de producto:** Aplicación web de simulación académica para la atención de emergencias médicas, despacho de ambulancias y gestión básica de recursos hospitalarios.
- **Naturaleza y propósito (`DEC-001`, `DEC-007`):** Proyecto académico de ingeniería de software cuyo objetivo es simular integralmente el ciclo de emergencias médicas —desde el registro de la emergencia hasta la llegada al hospital— con énfasis en concurrencia y asignación de recursos, bajo un **nivel de realismo intermedio** y una **representación geográfica simplificada inspirada en Arequipa**.
- **Restricción fundamental (`DEC-001`, `DEC-007`):** ARE es **exclusivamente un simulador académico**. No es un sistema clínico ni de despacho real, y no simula hospitalización, tratamientos ni atención médica real.

---

## 2. Contexto y problemática

### 2.1 Contexto del proceso simulado
La respuesta ante emergencias médicas prehospitalarias exige coordinar solicitudes entrantes con recursos limitados y heterogéneos (ambulancias básicas y especializadas, red vial y centros hospitalarios). En el contexto simulado de ARE, cada incidente registrado requiere una clasificación reproducible para determinar qué equipamiento necesita, evaluar qué ambulancias son elegibles, calcular costos de desplazamiento sobre una red vial simplificada inspirada en Arequipa, despachar la unidad adecuada preservando los recursos especializados y dar seguimiento al traslado hasta la llegada al hospital.

### 2.2 Problemática académica y de ingeniería de software
Modelar este proceso con fines académicos busca resolver y demostrar tres desafíos centrales de ingeniería de software:

1. **Concurrencia y competencia por recursos escasos:** Cuando ocurren solicitudes o asignaciones simultáneas (tanto en operación manual supervisada como en despacho automático o escenarios experimentales concurrentes), una ambulancia o una plaza hospitalaria simulada puede dejar de estar disponible en el instante en que se intenta asignar o reservar. El sistema debe impedir asignaciones simultáneas incompatibles de ambulancias y sobreocupación o doble reserva de plazas hospitalarias, reevaluando alternativas o ejecutando reintentos acotados manteniendo la consistencia en MySQL (`DEC-011`, `DEC-017`, `DEC-018`).
2. **Planificación de colas y observación de *starvation*:** Cuando no existen unidades elegibles y se acumulan emergencias pendientes con distintos requerimientos de equipamiento, las políticas de liberación y priorización de recursos pueden generar fenómenos de espera prolongada (*starvation*) que deben poder medirse y analizarse experimentalmente.
3. **Calidad arquitectónica y desacoplamiento:** Representar reglas configurables de triage, algoritmos de cálculo de rutas (Dijkstra) y políticas de despacho exige una arquitectura de monolito modular con principios hexagonales (`DEC-002`) y separación estricta entre el modelo de dominio y las entidades JPA (`DEC-003`).

---

## 3. Visión del producto

### 3.1 Declaración de visión (Aprobada en alcance base — `DEC-007`, `DEC-008`)
Construir un **simulador académico web con nivel de realismo intermedio** que represente integralmente el ciclo de atención de emergencias médicas desde el registro del incidente hasta la llegada de la ambulancia al hospital sobre una red geográfica simplificada inspirada en Arequipa, permitiendo operar despachos supervisados y automáticos, ejecutar escenarios experimentales concurrentes y analizar métricas de asignación y tiempos de espera sobre una arquitectura modular, hexagonal y transaccionalmente consistente.

### 3.2 Valor académico del simulador
- **Estudio de concurrencia y consistencia:** Verificación práctica de conflictos concurrentes, reintentos limitados y actualización de alternativas en tiempo de despacho.
- **Experimentación controlada y métricas:** Capacidad para que el Administrador configure escenarios experimentales y observe métricas operativas, incluyendo tiempos de espera y posibles situaciones de *starvation* en la cola de emergencias.
- **Separación de preocupaciones algorítmicas y transaccionales:** Distinción explícita entre los algoritmos de cálculo de rutas en grafos (Dijkstra en el MVP) y los mecanismos de coordinación y asignación concurrente.

---

## 4. Objetivo general

- **Objetivo general (`AGENTS.md` Sección 1.2, `DEC-007`):**
  Desarrollar un simulador académico que permita representar y gestionar integralmente el ciclo de atención de emergencias médicas —desde el registro de la solicitud hasta la llegada al hospital—, incluyendo la clasificación reproducible de emergencias, la gestión de ambulancias y hospitales, la asignación supervisada y automática con preservación de unidades especializadas, el cálculo de rutas mediante Dijkstra, el seguimiento simulado y la ejecución de escenarios concurrentes con recolección de métricas experimentales.

---

## 5. Objetivos específicos

1. **OE-01 (Dominio puro y arquitectura hexagonal — `DEC-002`, `DEC-003`):** Modelar el dominio del simulador en Java sin dependencias de Spring, JPA ni Hibernate, estructurado como un monolito modular con capas `domain`, `application`, `infrastructure` y `presentation`.
2. **OE-02 (Triage configurable y reproducible — `DEC-010`):** Determinar de forma reproducible y trazable la clasificación simulada y el equipamiento requerido de cada emergencia a partir de reglas configurables, sin emitir diagnósticos clínicos reales.
3. **OE-03 (Asignación inteligente y preservación de recursos — `DEC-011`):** Soportar el despacho en modalidades supervisada y automática evaluando elegibilidad previa, comparando costos de desplazamiento y priorizando el uso de ambulancias básicas cuando sean suficientes para preservar las unidades especializadas.
4. **OE-04 (Resiliencia ante concurrencia y gestión de cola — `DEC-011`, `DEC-012`):** Resolver fallos por asignaciones simultáneas mediante recálculo de alternativas (modo supervisado) o reintentos automáticos limitados (modo automático), y gestionar las emergencias pendientes por afinidad de equipamiento y orden FIFO al liberarse unidades.
5. **OE-05 (Ruteo con Dijkstra y experimentación — `DEC-008`, `DEC-012`, `DEC-013`):** Calcular rutas sobre un grafo vial simplificado de Arequipa mediante el algoritmo de Dijkstra, simular los desplazamientos hasta el hospital y proveer métricas experimentales sobre el desempeño y los tiempos de espera.

---

## 6. Usuarios o actores del sistema

> [!IMPORTANT]
> **Acuerdo aprobado (`DEC-009`):** Se establecen formalmente **tres actores humanos** y se define que el **motor de simulación es un componente interno del sistema**, no un actor humano. *(Nota de trazabilidad: esta decisión corrige y reemplaza la tabla preliminar de cinco actores de la versión inicial de descubrimiento).*

### 6.1 Actores humanos aprobados (`DEC-009`)

| ID | Actor humano | Responsabilidades aprobadas | Estado |
| :--- | :--- | :--- | :--- |
| **ACT-01** | **Operador de recepción** | Registra emergencias entrantes y consulta información y estados de las solicitudes. | **Aprobado** (`DEC-009`) |
| **ACT-02** | **Despachador** | Revisa emergencias, gestiona asignaciones (en modalidad supervisada o supervisando la automática) y supervisa los despachos activos. | **Aprobado** (`DEC-009`) |
| **ACT-03** | **Administrador** | Gestiona ambulancias, hospitales, reglas configurables de clasificación (triage) y parámetros del simulador; configura y ejecuta escenarios experimentales y consulta métricas. | **Aprobado** (`DEC-009`) |

### 6.2 Componentes internos del sistema (No actores humanos)

- **Motor de simulación (`CMP-INT-01` — Aprobado en `DEC-009`):** Componente interno del software (no un actor humano) encargado de materializar la ejecución de escenarios experimentales concurrentes, el avance de los desplazamientos simulados y la generación de eventos internos bajo los parámetros configurados por el Administrador.

*(Para el detalle completo de responsabilidades, interacciones y límites de cada actor, véase [`docs/product/stakeholders.md`](file:///c:/Users/giovani/Documents/GitHub/ProyectoFinalTO-Arequipa-RescUE/docs/product/stakeholders.md)).*

---

## 7. Descripción general del funcionamiento esperado

El funcionamiento aprobado para el ciclo simulado comprende las siguientes etapas y reglas operativas:

1. **Configuración de recursos y reglas (Administrador):**
   - Administración de ambulancias (disponibilidad, estado, ubicación en la red y equipamiento `BASIC` o `SPECIALIZED`), hospitales (ubicación, estado de habilitación y capacidad numérica simulada `C`, `R`, `O`, `D = C - R - O` con bloqueo de edición de capacidad durante escenarios experimentales activos según `DEC-018`), versiones inmutables de reglas configurables de clasificación (con bloqueo de publicación/activación durante escenarios experimentales activos según `DEC-016`) y parámetros del simulador.
2. **Registro y clasificación reproducible de la emergencia (Operador de recepción / Escenario experimental):**
   - Se registra la emergencia ingresando la categoría ficticia del incidente (obligatoria; catálogo inicial `INC-A`, `INC-B`, `INC-C`, `INC-D`), el nodo de ubicación en el grafo simplificado de Arequipa (obligatorio) y una descripción ficticia (opcional) (`DEC-016`).
   - El sistema consulta la versión activa de reglas configurables para determinar de forma **determinista, reproducible y trazable** la prioridad simulada informativa (`Alta`, `Media`, `Baja`) y la categoría de equipamiento requerido (`BASIC` o `SPECIALIZED`), conservando en la emergencia la referencia a la versión aplicada sin emitir diagnósticos clínicos reales (`DEC-010`, `DEC-016`). La prioridad simulada es exclusivamente informativa y no altera el orden FIFO, la preservación de ambulancias especializadas, la elegibilidad ni el costo de ruta por Dijkstra (`DEC-016`).
3. **Determinación de elegibilidad y recomendación de ambulancias (`DEC-011`, `DEC-013`, `DEC-014`):**
   - Antes de emitir cualquier recomendación, el sistema filtra las ambulancias **elegibles** según disponibilidad, estado operativo y equipamiento requerido.
   - **La disponibilidad hospitalaria no interviene en la selección inicial de ambulancias** (`DEC-014`). *(Nota de trazabilidad: se corrige el supuesto preliminar de la versión anterior que vinculaba ambos pasos en la selección inicial).*
   - Se aplica la política de **preservación de recursos especializados**: se prefieren ambulancias básicas cuando sean suficientes; una ambulancia especializada solo se considera para una emergencia básica si no existen alternativas básicas elegibles y la especializada cumple los requisitos (`DEC-011`).
   - Entre las ambulancias elegibles del grupo correspondiente, el sistema calcula y compara el costo de desplazamiento utilizando el algoritmo de **Dijkstra** sobre el grafo vial simplificado inicial de 10 nodos con costos abstractos (`DEC-013`, `DEC-019`, `RN-RUT-006`) y aplica desempate determinista por menor identificador técnico estable ante igualdad de costo (`DEC-019`, `RN-ASI-005`).
4. **Asignación y manejo de concurrencia (`DEC-011`, `DEC-017`):**
   - **Si no existen ambulancias elegibles:** la emergencia permanece en estado **pendiente** (consumiendo `0` reintentos de concurrencia cuando ocurre desde el inicio).
   - **Modalidad supervisada:** el sistema presenta las alternativas elegibles recomendadas y el Despachador puede seleccionar **cualquier ambulancia elegible**, aunque no sea la primera recomendada. Si al intentar confirmar la asignación la ambulancia seleccionada dejó de estar disponible por una operación concurrente, el sistema no confirma la asignación, actualiza las alternativas elegibles y solicita una nueva selección al Despachador (`DEC-011`, `DEC-017`, `RN-CON-002`).
   - **Modalidad automática:** el sistema selecciona y asigna automáticamente sin confirmación humana. Si la asignación falla por un conflicto concurrente de disponibilidad, cada operación automática dispone de **1 intento inicial y hasta 3 reintentos automáticos adicionales (máximo 4 intentos totales)**, reevaluando obligatoriamente en cada reintento la disponibilidad, elegibilidad, compatibilidad de equipamiento (`BASIC`/`SPECIALIZED`), preservación de unidades especializadas y costo de ruta por Dijkstra (`DEC-017`). El proceso se detiene de inmediato si se confirma una asignación válida o si tras una reevaluación ya no quedan ambulancias elegibles (sin consumir los reintentos restantes). Si el intento inicial y los 3 reintentos adicionales se agotan por conflictos concurrentes, la emergencia permanece en estado **pendiente**, se registra funcionalmente el agotamiento de reintentos por concurrencia y se notifica al Despachador mediante un aviso visible en la interfaz web interna del sistema (`DEC-017`, `RN-CON-005` a `RN-CON-009`).
5. **Atención de la cola de emergencias pendientes al liberarse unidades (`DEC-012`, `DEC-019`):**
   - Cuando se libera una ambulancia especializada (`SPECIALIZED`), primero se evalúan las emergencias pendientes (`WAITING_AMBULANCE`) que requieren específicamente ese equipamiento; solo si no existen emergencias en ese grupo se consideran las emergencias básicas compatibles (`RN-COL-001`, `RN-COL-002`).
   - Cuando se libera una ambulancia básica (`BASIC`), se evalúan únicamente las emergencias pendientes (`WAITING_AMBULANCE`) que requieren equipamiento `BASIC`, seleccionando la emergencia elegible más antigua (`DEC-019`, `RN-COL-007`); si no existe emergencia compatible, la ambulancia permanece disponible (`AVAILABLE`).
   - Dentro de cada grupo se respeta el orden **FIFO** según el instante de ingreso a espera, sin aplicar *aging* automático en la primera versión y sin que la prioridad simulada informativa (`Alta`/`Media`/`Baja`) altere el orden (`DEC-012`, `DEC-016`, `DEC-019`).
6. **Selección de hospital, reserva concurrente, desplazamiento simulado, liberación de ambulancia y fin del ciclo en la llegada al hospital (`DEC-007`, `DEC-014`, `DEC-018`, `DEC-019`):**
   - Al **confirmar la recogida de la emergencia por la ambulancia** (`AT_INCIDENT`), el sistema selecciona automáticamente el hospital elegible (habilitado, alcanzable en el grafo y con al menos una plaza disponible `D >= 1`) de **menor costo de ruta mediante Dijkstra** desde el nodo del incidente, aplicando desempate determinista por identificador técnico estable y confirmando el destino junto con la reserva de plaza (`R`) bajo una política de **1 intento inicial + hasta 2 reintentos automáticos adicionales ante conflictos concurrentes (máximo 3 intentos totales)** (`DEC-018`, `RN-HOS-004` a `RN-HOS-008`).
   - No se permite iniciar el traslado sin destino hospitalario confirmado. Si no existen hospitales elegibles o se agotan los reintentos concurrentes (`HOSPITAL_RESERVATION_RETRY_EXHAUSTED`), la emergencia permanece activa en estado `WAITING_HOSPITAL` conservando su ambulancia asignada y ocupada (`AT_INCIDENT`) y entra en una **cola FIFO de espera de destino hospitalario**, la cual se reevalúa automáticamente sin pérdida de posición cuando se libera capacidad mediante un **evento explícito de alta simulada** (`RN-HOS-009` a `RN-HOS-013`).
   - Una vez confirmado el destino (`IN_TRANSPORT`), el motor de simulación por **eventos discretos con avance manual** y tiempo lógico monotónico (`RN-RUT-009`) avanza el desplazamiento hasta la **llegada al hospital y entrega simulada**, hito donde la emergencia pasa a `COMPLETED`, la reserva se transforma en ocupación (`R` disminuye en 1, `O` aumenta en 1 y `D` no cambia) y **la ambulancia se libera pasando a `AVAILABLE`** (`DEC-018`, `DEC-019`, `RN-RUT-007`, `RN-RUT-008`, `RN-HOS-012`, `RN-HOS-014`), permaneciendo ocupada la plaza hospitalaria hasta el evento explícito de alta simulada.

---

## 8. Alcance funcional del producto

### 8.1 Alcance confirmado del MVP (`DEC-008`)

Los siguientes ocho componentes forman parte **aprobada y confirmada** del MVP de ARE:

| # | Componente del MVP | Alcance funcional aprobado | Estado |
| :--- | :--- | :--- | :--- |
| **1** | **Gestión de emergencias** | Registro de emergencias, consulta de información/estados, clasificación reproducible y trazable mediante reglas configurables (`DEC-010`) y gestión de la cola de pendientes (`DEC-012`). | **Aprobado en MVP** |
| **2** | **Gestión de ambulancias** | Administración de unidades (básicas y especializadas), ubicación en el grafo, equipamiento, disponibilidad y estados operativos. | **Aprobado en MVP** |
| **3** | **Gestión básica de hospitales** | Administración de hospitales con ubicación en el grafo y capacidad numérica simulada (`C`, `R`, `O`, `D = C - R - O` con `R + O <= C`), sin intervenir en la selección inicial de ambulancias (`DEC-014`), con selección por menor costo Dijkstra tras la recogida, reserva concurrente (1 intento inicial + hasta 2 reintentos), cola FIFO de espera de destino, transformación de reserva en ocupación al llegar y liberación por alta simulada (`DEC-018`). | **Aprobado en MVP** |
| **4** | **Asignación supervisada y automática** | Evaluación previa de elegibilidad, comparación por costo de desplazamiento, preservación de unidades especializadas, selección flexible por el despachador en modo supervisado y asignación directa con reintentos limitados en modo automático (`DEC-011`). | **Aprobado en MVP** |
| **5** | **Rutas mediante Dijkstra** | Grafo ficticio ponderado inicial de 10 nodos (`N01` a `N10`) inspirado en zonas de Arequipa con costos enteros positivos en unidades abstractas y cálculo de rutas mediante Dijkstra (`DEC-007`, `DEC-013`, `DEC-019`, `RN-RUT-006`). | **Aprobado en MVP** |
| **6** | **Seguimiento simulado** | Máquina mínima de 7 estados de emergencia y 4 de ambulancia, avance manual por eventos discretos con tiempo lógico monotónico y liberación de ambulancia al llegar al hospital (`DEC-007`, `DEC-019`, `RN-RUT-007` a `RN-RUT-009`). | **Aprobado en MVP** |
| **7** | **Simulación concurrente** | Configuración y ejecución de escenarios experimentales con eventos/solicitudes simultáneas gestionados por el motor interno de simulación y control de consistencia ante conflictos (`DEC-009`, `DEC-011`, `DEC-017` a `DEC-019`). | **Aprobado en MVP** |
| **8** | **Métricas experimentales** | Registro experimental trazable (`RN-MET-003`) y catálogo reducido de 7 métricas (`RN-MET-004`), incluyendo tiempos de espera en cola de ambulancia y de hospital, utilización y conteo de conflictos/reintentos (`DEC-008`, `DEC-009`, `DEC-012`, `DEC-019`). | **Aprobado en MVP** |

### 8.2 Propuestas o extensiones futuras (No obligatorias en el MVP)
- **Algoritmo A\* (`DEC-008`, `DEC-013`):** Contemplado únicamente como posible extensión futura o comparación experimental frente a Dijkstra; **no es requisito obligatorio del MVP**.
- **Envejecimiento automático (*aging*) en cola (`DEC-012`):** Excluido de la primera versión para permitir observar el comportamiento puro de la cola y posibles situaciones de *starvation*; podría evaluarse en versiones posteriores.

### 8.3 Aspectos funcionales pendientes de definición
- **Clasificación simulada y equipamiento (`PEN-002a` — Cerrado por `DEC-016`):** Quedaron aprobados el catálogo inicial (`INC-A` a `INC-D`), las categorías de equipamiento (`BASIC`, `SPECIALIZED`), el carácter informativo de la prioridad simulada, el versionado inmutable de reglas (con bloqueo de publicación/activación durante escenarios activos), los campos del registro y sus validaciones (`RN-EME-007` a `RN-EME-012`). Permanece pendiente de definición si se permitirá o no preparar borradores de reglas durante escenarios activos (`stakeholders.md` Secc. 5.2).
- **Límite y política funcional de reintentos automáticos (`PEN-002b` — Cerrado por `DEC-017`):** Quedó aprobada la política de **1 intento inicial + hasta 3 reintentos automáticos adicionales (máximo 4 intentos totales)** por operación de asignación automática ante conflictos concurrentes, con reevaluación obligatoria, finalización anticipada, permanencia en estado `pendiente` al agotar el límite, registro funcional del resultado y aviso visible al `Despachador` en la interfaz web (`RN-CON-005` a `RN-CON-009`, `CA-CON-01` a `CA-CON-10`). Los mecanismos técnicos de concurrencia, bloqueo, aislamiento y transacciones permanecen pendientes en `PEN-006`.
- **Selección de hospital, capacidad hospitalaria, reservas concurrentes y alta simulada (`PEN-002c` — Cerrado por `DEC-018`):** Quedaron aprobados el modelo de capacidad numérica simulada (`C`, `R`, `O`, `D = C - R - O` con `R + O <= C`), la selección automática al confirmar la recogida usando Dijkstra y desempate determinista, la confirmación del destino junto con la reserva de plaza (1 intento inicial + hasta 2 reintentos ante conflictos concurrentes = máximo 3 intentos totales), la espera de destino con cola FIFO y reevaluación automática sin pérdida de posición, la transformación de reserva en ocupación a la llegada, el alta simulada explícita, la independencia entre liberación de plaza y de ambulancia y la prohibición de editar la capacidad hospitalaria durante escenarios activos (`RN-HOS-004` a `RN-HOS-015`, `CA-HOS-01` a `CA-HOS-13`). Permanecen pendientes de definición la cancelación/reasignación de reservas confirmadas y los permisos exactos del actor para activar el alta simulada.
- **Cola al liberar ambulancias básicas, desempate, estados, liberación de ambulancia, grafo, tiempo simulado y métricas (`PEN-002d` — Cerrado por `DEC-019`):** Quedaron aprobados la evaluación FIFO de emergencias `BASIC` al liberarse una ambulancia `BASIC` (`RN-COL-007`), el desempate determinista por menor identificador técnico estable (`RN-ASI-005`), los 7 estados de emergencia y 4 de ambulancia (`RN-RUT-007`), la liberación de ambulancia al confirmar la llegada y entrega simulada en el hospital (`RN-RUT-008`), el grafo inicial de 10 nodos con costos abstractos (`RN-RUT-006`), la simulación por eventos discretos con avance manual y tiempo lógico monotónico (`RN-RUT-009`), el registro experimental mínimo (`RN-MET-003`) y el catálogo de 7 métricas (`RN-MET-004`, `CA-SIM-01` a `CA-SIM-10`).

---

## 9. Elementos explícitamente fuera del alcance

De acuerdo con `DEC-001`, `DEC-002`, `DEC-007` y `DEC-008`, quedan **explícitamente fuera del alcance del MVP y/o del proyecto**:

1. **Despacho real de ambulancias y operación en emergencias reales (`DEC-001`).**
2. **Decisiones clínicas reales, diagnóstico médico, hospitalización o tratamiento médico (`DEC-001`, `DEC-007`, `DEC-008`).**
3. **Datos reales de pacientes (`DEC-008`).**
4. **GPS y telemetría reales (`DEC-008`).**
5. **Tráfico real en tiempo real (`DEC-008`).**
6. **Telefonía real o integración con centrales de llamadas (`DEC-008`).**
7. **Arquitectura de microservicios (`DEC-002`, `DEC-008`).**
8. **Algoritmo A\* como requisito obligatorio del MVP (`DEC-008`, `DEC-013`).**

---

## 10. Restricciones y supuestos

### 10.1 Restricciones obligatorias (Aprobadas)
- **Arquitectónicas (`DEC-002`, `DEC-003`, `DEC-020` / `ADR-0001`):**
  - Monolito modular en un único proyecto Maven bajo `com.arequiparescue` con 7 módulos funcionales (`emergency`, `ambulance`, `dispatch`, `routing`, `hospital`, `simulation`, `reporting`) más `shared`, aplicando principios de arquitectura hexagonal (`domain`, `application`, `infrastructure`, `presentation`) y MVC (Spring MVC + Thymeleaf) en la capa de presentación.
  - Independencia total del paquete `domain/` (sin anotaciones ni dependencias de Spring, JPA, Hibernate o Servlet), prohibición de exponer entidades JPA en controladores o vistas y migraciones SQL versionadas con Flyway (`ddl-auto=validate`).
- **Tecnológicas (`DEC-005`, `DEC-020` / `ADR-0001`):**
  - Stack fijo y versiones aprobadas: Java `21` LTS, Spring Boot `3.4.5` (BOM oficial con Spring MVC `6.2.x`, Thymeleaf `3.1.3.RELEASE`, Spring Data JPA `3.4.x` / Hibernate `6.6.x`, Flyway `10.20.x`, `mysql-connector-j`), MySQL `8.4` LTS y Apache Maven `3.9.9` mediante Maven Wrapper (`mvnw`).
- **Funcionales y de diseño algorítmico (`DEC-010` a `DEC-014`):**
  - No confundir la necesidad técnica de equipamiento con prioridad clínica (`DEC-012`).
  - No confundir el algoritmo de cálculo de rutas (Dijkstra) con los mecanismos de asignación concurrente (`DEC-013`).
  - No hacer depender la selección inicial de ambulancias de la disponibilidad hospitalaria (`DEC-014`).
- **Idioma (`DEC-004`):**
  - Documentación, ADR e interfaz de usuario en español neutro/profesional; identificadores técnicos de código en inglés.

### 10.2 Supuestos preliminares (Sujetos a validación)
- **SUP-01:** El entorno académico contará con una base de datos MySQL local o contenedorizada para las pruebas transaccionales y de concurrencia.
- **SUP-02:** El grafo vial simplificado de Arequipa se cargará a partir de datos estructurados controlados por el simulador (nodos de zonas/puntos clave y aristas con pesos positivos compatibles con Dijkstra).
- **SUP-03:** La interacción de los tres actores humanos (`Operador de recepción`, `Despachador` y `Administrador`) se realizará mediante vistas web renderizadas con Spring MVC y Thymeleaf.

---

## 11. Riesgos y aspectos pendientes de definición

### 11.1 Riesgos identificados

| ID | Riesgo | Categoría | Impacto potencial | Mitigación prevista |
| :--- | :--- | :--- | :--- | :--- |
| **RIE-01** | **Colisiones de concurrencia en despacho simultáneo** | Técnico / Concurrencia | Dos operaciones concurrentes pueden intentar tomar la misma ambulancia disponible. | Aplicación de límites transaccionales, reevaluación de alternativas en modo supervisado, reintentos acotados en modo automático (`DEC-011`) y mecanismos de control en BD/JPA definidos por ADR (`PEN-006`). |
| **RIE-02** | **Inanición (*starvation*) de emergencias en cola** | Dominio / Planificación | Al no aplicar *aging* automático en v1 y priorizar emergencias con requerimiento específico cuando se libera una ambulancia especializada (`DEC-012`), ciertas solicitudes podrían experimentar altos tiempos de espera bajo saturación. | Riesgo aceptado con propósito académico: se registrarán tiempos de espera y métricas específicas para observar y estudiar empíricamente las situaciones de *starvation* (`RN-COL-005`, `RN-MET-002`). |
| **RIE-03** | **Fuga de detalles de infraestructura hacia el dominio o la vista** | Arquitectónico | Acoplar el dominio a JPA/Spring o exponer entidades JPA en Thymeleaf, incumpliendo `DEC-003`. | Mapeadores explícitos en `infrastructure/`, DTOs en `presentation/` y pruebas unitarias puras en `domain/`. |
| **RIE-04** | **Confusión entre cálculo de rutas y control de concurrencia** | Diseño / Dominio | Mezclar la lógica del algoritmo de Dijkstra con el bloqueo o reserva transaccional de unidades. | Separar el servicio/puerto de cálculo de rutas sobre el grafo respecto del caso de uso transaccional de asignación (`DEC-013`). |
| **RIE-05** | **Divergencia de rutas documentales respecto a `AGENTS.md` Secc. 8.2/8.3** | Documental | `AGENTS.md` ilustra como propuesta inicial `docs/project/` y `docs/adr/`, mientras que la estructura autorizada utiliza `docs/product/` y `docs/decisions/adr/`. | Registrado en `PEN-008` para actualizar las Secciones 8.2 y 8.3 de `AGENTS.md` cuando el responsable humano lo autorice. |

### 11.2 Resumen de definiciones pendientes antes de la implementación
1. *(Resuelto por `DEC-016`)* Modelo de clasificación simulada, catálogo inicial `INC-A` a `INC-D`, equipamiento `BASIC`/`SPECIALIZED`, versionado inmutable y validaciones de registro (`PEN-002a` cerrado; queda pendiente definir la preparación de borradores durante escenarios activos en `stakeholders.md` Secc. 5.2).
2. *(Resuelto por `DEC-017`)* Política funcional y límite máximo de reintentos en asignación automática ante fallos concurrentes: 1 intento inicial + hasta 3 reintentos automáticos adicionales (`PEN-002b` cerrado; los mecanismos técnicos de concurrencia corresponden a `PEN-006`).
3. *(Resuelto por `DEC-018`)* Reglas de selección de hospital tras confirmar recogida, capacidad numérica simulada (`C`, `R`, `O`, `D`), reserva concurrente (1 intento inicial + hasta 2 reintentos), cola FIFO de espera de destino, transformación en ocupación al llegar y alta simulada (`PEN-002c` cerrado; quedan diferidos la cancelación/reasignación de reservas confirmadas y los permisos exactos para el alta simulada en `stakeholders.md` Secc. 5, y los mecanismos técnicos de concurrencia en `PEN-006`).
4. *(Resuelto por `DEC-019`)* Comportamiento de la cola al liberarse una ambulancia básica (`RN-COL-007`), criterio de desempate determinista entre ambulancias elegibles de igual costo (`RN-ASI-005`), catálogo de 7 estados de emergencia y 4 de ambulancia (`RN-RUT-007`), liberación de ambulancia a la llegada al hospital (`RN-RUT-008`), grafo inicial de 10 nodos de Arequipa (`RN-RUT-006`), simulación por eventos discretos con avance manual (`RN-RUT-009`), registro experimental trazable (`RN-MET-003`) y catálogo de 7 métricas (`RN-MET-004`) (`PEN-002d` cerrado).
5. *(Resuelto parcialmente por `DEC-020` / `ADR-0001`)* Versiones concretas del stack (`PEN-003` cerrado), estructura modular hexagonal (`PEN-004` cerrado), persistencia, identificadores, transacciones y migraciones con Flyway (`PEN-005` cerrado) y estrategia de pruebas (`PEN-007` cerrado). Permanecen pendientes únicamente el ADR de mecanismos técnicos de concurrencia en BD y aplicación (`PEN-006`) y la alineación de rutas documentales en `AGENTS.md` (`PEN-008`).

---

## 12. Criterios preliminares de éxito académico

1. **Fidelidad funcional del ciclo aprobado:** El simulador ejecuta íntegramente el flujo desde el registro y clasificación reproducible de la emergencia hasta la llegada de la ambulancia al hospital sobre la red simplificada de Arequipa, cubriendo los 8 componentes del MVP (`DEC-007`, `DEC-008`).
2. **Verificación de concurrencia y políticas de despacho de ambulancias:** Se comprueba mediante pruebas automatizadas y escenarios experimentales que:
   - Ninguna ambulancia es asignada simultáneamente a dos emergencias activas incompatibles.
   - En modo supervisado se refrescan alternativas cuando la unidad elegida deja de estar disponible, y en modo automático se reintenta hasta el límite configurado (`DEC-011`).
   - La liberación de ambulancias especializadas y básicas respeta el orden por equipamiento requerido y FIFO sin *aging* (`DEC-012`, `DEC-019`), la liberación de la ambulancia ocurre al completar la llegada y entrega simulada en el hospital (`DEC-019`), y la selección, reserva concurrente, cola FIFO de espera de destino, ocupación y alta simulada de hospitales cumplen las invariantes de capacidad (`R + O <= C`) y los criterios `CA-HOS-01` a `CA-HOS-13` y `CA-SIM-01` a `CA-SIM-10` (`DEC-018`, `DEC-019`).
3. **Observabilidad experimental:** El Administrador puede configurar y ejecutar escenarios concurrentes y consultar métricas que evidencien tiempos de espera, asignaciones y posibles casos de *starvation*.
4. **Pureza arquitectónica (`DEC-002`, `DEC-003`):** El modelo de dominio se mantiene 100 % libre de dependencias de Spring, JPA y Hibernate y cuenta con pruebas unitarias aisladas.
5. **Trazabilidad extremo a extremo:** Coherencia verificable entre `AGENTS.md`, `decision-log.md`, `vision-and-scope.md`, `stakeholders.md`, `business-rules.md`, los requisitos funcionales/no funcionales, los ADR y el código final.

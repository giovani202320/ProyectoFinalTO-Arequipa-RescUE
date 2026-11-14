# Visión y Alcance del Producto — ARE (Arequipa RescUE)

**Estado del documento:** Aprobado — Versión inicial aceptada (`DEC-015`) actualizada con la formalización de `PEN-002a` (`DEC-016`; parámetros y decisiones pendientes en `PEN-002b` a `PEN-008`)
**Naturaleza del documento:** Documento de visión y alcance del producto en su versión inicial aceptada (`DEC-015`) y actualizada con `DEC-016`, que incorpora formalmente los acuerdos funcionales y de MVP aprobados por el responsable del proyecto (`DEC-001` a `DEC-016`), distinguiéndolos de las propuestas y parámetros que continúan pendientes de definición (`PEN-002b` a `PEN-008`).

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

1. **Concurrencia y competencia por recursos escasos:** Cuando ocurren solicitudes o asignaciones simultáneas (tanto en operación manual supervisada como en despacho automático o escenarios experimentales concurrentes), una ambulancia puede dejar de estar disponible en el instante en que se intenta asignar. El sistema debe impedir asignaciones simultáneas incompatibles de ambulancias, reevaluar alternativas o ejecutar reintentos acotados manteniendo la consistencia en MySQL (`DEC-011`). *(Nota de alcance: las reglas de selección de hospital, capacidad hospitalaria simulada y eventual reserva de camas no forman parte de los acuerdos cerrados actuales y permanecen pendientes de definición en `PEN-002c`).*
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
   - Administración de ambulancias (disponibilidad, estado, ubicación en la red y equipamiento `BASIC` o `SPECIALIZED`), hospitales (ubicación y disponibilidad simulada), versiones inmutables de reglas configurables de clasificación (con bloqueo de publicación/activación durante escenarios experimentales activos según `DEC-016`) y parámetros del simulador.
2. **Registro y clasificación reproducible de la emergencia (Operador de recepción / Escenario experimental):**
   - Se registra la emergencia ingresando la categoría ficticia del incidente (obligatoria; catálogo inicial `INC-A`, `INC-B`, `INC-C`, `INC-D`), el nodo de ubicación en el grafo simplificado de Arequipa (obligatorio) y una descripción ficticia (opcional) (`DEC-016`).
   - El sistema consulta la versión activa de reglas configurables para determinar de forma **determinista, reproducible y trazable** la prioridad simulada informativa (`Alta`, `Media`, `Baja`) y la categoría de equipamiento requerido (`BASIC` o `SPECIALIZED`), conservando en la emergencia la referencia a la versión aplicada sin emitir diagnósticos clínicos reales (`DEC-010`, `DEC-016`). La prioridad simulada es exclusivamente informativa y no altera el orden FIFO, la preservación de ambulancias especializadas, la elegibilidad ni el costo de ruta por Dijkstra (`DEC-016`).
3. **Determinación de elegibilidad y recomendación de ambulancias (`DEC-011`, `DEC-013`, `DEC-014`):**
   - Antes de emitir cualquier recomendación, el sistema filtra las ambulancias **elegibles** según disponibilidad, estado operativo y equipamiento requerido.
   - **La disponibilidad hospitalaria no interviene en la selección inicial de ambulancias** (`DEC-014`). *(Nota de trazabilidad: se corrige el supuesto preliminar de la versión anterior que vinculaba ambos pasos en la selección inicial).*
   - Se aplica la política de **preservación de recursos especializados**: se prefieren ambulancias básicas cuando sean suficientes; una ambulancia especializada solo se considera para una emergencia básica si no existen alternativas básicas elegibles y la especializada cumple los requisitos (`DEC-011`).
   - Entre las ambulancias elegibles del grupo correspondiente, el sistema calcula y compara el costo de desplazamiento utilizando el algoritmo de **Dijkstra** sobre el grafo vial simplificado (`DEC-013`).
4. **Asignación y manejo de concurrencia (`DEC-011`):**
   - **Si no existen ambulancias elegibles:** la emergencia permanece en estado **pendiente**.
   - **Modalidad supervisada:** el sistema presenta las alternativas elegibles recomendadas y el Despachador puede seleccionar **cualquier ambulancia elegible**, aunque no sea la primera recomendada. Si al intentar confirmar la asignación la ambulancia seleccionada dejó de estar disponible por una operación concurrente, el sistema actualiza las alternativas elegibles y solicita una nueva selección al Despachador.
   - **Modalidad automática:** el sistema selecciona y asigna automáticamente sin confirmación humana. Si la asignación concurrente falla porque la ambulancia dejó de estar disponible, el sistema reevalúa las alternativas y reintenta automáticamente con otra ambulancia elegible hasta un número máximo de reintentos (valor pendiente de definición); si los reintentos se agotan sin éxito, la emergencia permanece **pendiente**.
5. **Atención de la cola de emergencias pendientes al liberarse unidades (`DEC-012`):**
   - Cuando se libera una ambulancia especializada, primero se evalúan las emergencias pendientes que requieren específicamente ese equipamiento; solo si no existen emergencias en ese grupo se consideran las emergencias básicas compatibles.
   - Dentro de cada grupo se respeta el orden **FIFO** según el instante de registro de la emergencia, sin aplicar *aging* automático en la primera versión.
   - El comportamiento específico de la cola cuando se libera una **ambulancia básica** permanece pendiente de definición funcional (`PEN-002d`).
6. **Desplazamiento simulado y fin del ciclo en la llegada al hospital (`DEC-007`, `DEC-014`):**
   - Una vez asignada la unidad, se simula su desplazamiento por la ruta calculada y el seguimiento de estados hasta la **llegada al hospital**, punto en el que concluye el ciclo simulado de atención de la emergencia, registrándose las métricas operativas y tiempos de espera correspondientes. Las reglas específicas para elegir el hospital de destino, gestionar su capacidad simulada o reservar camas permanecen pendientes de definición (`PEN-002c`).

---

## 8. Alcance funcional del producto

### 8.1 Alcance confirmado del MVP (`DEC-008`)

Los siguientes ocho componentes forman parte **aprobada y confirmada** del MVP de ARE:

| # | Componente del MVP | Alcance funcional aprobado | Estado |
| :--- | :--- | :--- | :--- |
| **1** | **Gestión de emergencias** | Registro de emergencias, consulta de información/estados, clasificación reproducible y trazable mediante reglas configurables (`DEC-010`) y gestión de la cola de pendientes (`DEC-012`). | **Aprobado en MVP** |
| **2** | **Gestión de ambulancias** | Administración de unidades (básicas y especializadas), ubicación en el grafo, equipamiento, disponibilidad y estados operativos. | **Aprobado en MVP** |
| **3** | **Gestión básica de hospitales** | Administración básica de hospitales, ubicación en el grafo y disponibilidad simulada, sin intervenir en la selección inicial de ambulancias (`DEC-014`). Las reglas de selección de hospital, capacidad hospitalaria y eventual reserva de camas están pendientes de definición (`PEN-002c`). | **Aprobado en MVP** |
| **4** | **Asignación supervisada y automática** | Evaluación previa de elegibilidad, comparación por costo de desplazamiento, preservación de unidades especializadas, selección flexible por el despachador en modo supervisado y asignación directa con reintentos limitados en modo automático (`DEC-011`). | **Aprobado en MVP** |
| **5** | **Rutas mediante Dijkstra** | Representación geográfica simplificada inspirada en Arequipa y cálculo inicial de rutas/costos mediante el algoritmo de Dijkstra (`DEC-007`, `DEC-013`). | **Aprobado en MVP** |
| **6** | **Seguimiento simulado** | Representación y actualización del desplazamiento simulado y transiciones de estado desde el despacho hasta la llegada al hospital (`DEC-007`). | **Aprobado en MVP** |
| **7** | **Simulación concurrente** | Configuración y ejecución de escenarios experimentales con eventos/solicitudes simultáneas gestionados por el motor interno de simulación y control de consistencia ante conflictos (`DEC-009`, `DEC-011`). | **Aprobado en MVP** |
| **8** | **Métricas experimentales** | Registro y consulta de métricas de los escenarios ejecutados, incluyendo tiempos de espera en cola para observar posibles situaciones de *starvation* (`DEC-008`, `DEC-009`, `DEC-012`). | **Aprobado en MVP** |

### 8.2 Propuestas o extensiones futuras (No obligatorias en el MVP)
- **Algoritmo A\* (`DEC-008`, `DEC-013`):** Contemplado únicamente como posible extensión futura o comparación experimental frente a Dijkstra; **no es requisito obligatorio del MVP**.
- **Envejecimiento automático (*aging*) en cola (`DEC-012`):** Excluido de la primera versión para permitir observar el comportamiento puro de la cola y posibles situaciones de *starvation*; podría evaluarse en versiones posteriores.

### 8.3 Aspectos funcionales pendientes de definición
- **Clasificación simulada y equipamiento (`PEN-002a` — Cerrado por `DEC-016`):** Quedaron aprobados el catálogo inicial (`INC-A` a `INC-D`), las categorías de equipamiento (`BASIC`, `SPECIALIZED`), el carácter informativo de la prioridad simulada, el versionado inmutable de reglas (con bloqueo de publicación/activación durante escenarios activos), los campos del registro y sus validaciones (`RN-EME-007` a `RN-EME-012`). Permanece pendiente de definición si se permitirá o no preparar borradores de reglas durante escenarios activos (`stakeholders.md` Secc. 5.2).
- **Límite de reintentos automáticos (`PEN-002b`):** Número máximo de reintentos cuando una asignación automática falla por concurrencia.
- **Selección de hospital, capacidad hospitalaria y reserva de camas (`PEN-002c`):** Reglas para elegir el hospital de destino, administrar su capacidad simulada y definir si existirá o no reserva de camas durante el ciclo (no se asume la reserva concurrente de camas como comportamiento aprobado).
- **Comportamiento de cola al liberar ambulancias básicas, desempate, estados, grafo y métricas (`PEN-002d`):** Comportamiento de la cola cuando se libera una ambulancia básica, criterios de desempate ante igualdad de costo (`RN-ASI-005`), nombres exactos de los estados de emergencia y ambulancia, topología concreta del grafo simplificado de Arequipa, mecánica de avance del reloj de simulación y catálogo detallado de métricas experimentales (`RN-MET-003`, `RN-MET-004`).

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
- **Arquitectónicas (`DEC-002`, `DEC-003`):**
  - Monolito modular con principios de arquitectura hexagonal y MVC (Spring MVC + Thymeleaf) en la capa de presentación.
  - Independencia total del paquete `domain/` (sin anotaciones ni dependencias de Spring, JPA, Hibernate o Servlet) y prohibición de exponer entidades JPA en controladores o vistas.
- **Tecnológicas (`DEC-005`):**
  - Stack fijo: Java, Spring Boot, Spring MVC, Thymeleaf, Spring Data JPA, Hibernate, MySQL y Maven (versiones específicas pendientes de aprobación).
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
2. Número máximo de reintentos en asignación automática ante fallos concurrentes (`PEN-002b`).
3. Reglas de selección de hospital, administración de su capacidad simulada y eventual reserva de camas hospitalarias (`PEN-002c`).
4. Comportamiento de la cola al liberarse una ambulancia básica, criterio de desempate entre ambulancias elegibles de igual costo (`RN-ASI-005`), catálogo exacto de estados, nodos/aristas del grafo de Arequipa, modelo de avance de tiempo simulado y métricas experimentales detalladas (`RN-MET-003`, `RN-MET-004`) (`PEN-002d`).
5. Versiones concretas del stack (`PEN-003`) y ADR arquitectónicos pendientes (`PEN-004` a `PEN-007`).

---

## 12. Criterios preliminares de éxito académico

1. **Fidelidad funcional del ciclo aprobado:** El simulador ejecuta íntegramente el flujo desde el registro y clasificación reproducible de la emergencia hasta la llegada de la ambulancia al hospital sobre la red simplificada de Arequipa, cubriendo los 8 componentes del MVP (`DEC-007`, `DEC-008`).
2. **Verificación de concurrencia y políticas de despacho de ambulancias:** Se comprueba mediante pruebas automatizadas y escenarios experimentales que:
   - Ninguna ambulancia es asignada simultáneamente a dos emergencias activas incompatibles.
   - En modo supervisado se refrescan alternativas cuando la unidad elegida deja de estar disponible, y en modo automático se reintenta hasta el límite configurado (`DEC-011`).
   - La liberación de ambulancias especializadas respeta el orden por grupo de equipamiento requerido y FIFO sin *aging* (`DEC-012`). *(Las reglas de capacidad/reserva hospitalaria y de liberación de ambulancias básicas se verificarán una vez que se definan y aprueben `PEN-002c` y `PEN-002d`).*
3. **Observabilidad experimental:** El Administrador puede configurar y ejecutar escenarios concurrentes y consultar métricas que evidencien tiempos de espera, asignaciones y posibles casos de *starvation*.
4. **Pureza arquitectónica (`DEC-002`, `DEC-003`):** El modelo de dominio se mantiene 100 % libre de dependencias de Spring, JPA y Hibernate y cuenta con pruebas unitarias aisladas.
5. **Trazabilidad extremo a extremo:** Coherencia verificable entre `AGENTS.md`, `decision-log.md`, `vision-and-scope.md`, `stakeholders.md`, `business-rules.md`, los requisitos funcionales/no funcionales, los ADR y el código final.

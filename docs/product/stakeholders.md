# Actores e Interesados del Sistema — ARE (Arequipa RescUE)

**Estado del documento:** Aprobado — Versión inicial aceptada (`DEC-009`, `DEC-015`) actualizada con la formalización de `PEN-002a` (`DEC-016`; aspectos de Sección 5 pendientes de definición)
**Propósito:** Especificar los actores humanos que interactúan con el simulador académico **ARE – Arequipa RescUE**, sus responsabilidades aprobadas, sus límites operativos y la distinción explícita frente a los componentes internos del sistema.

> [!IMPORTANT]
> De acuerdo con `DEC-009`, el sistema cuenta con **tres actores humanos** (`Operador de recepción`, `Despachador` y `Administrador`). El **motor de simulación** es un componente interno del software y no constituye un actor humano.

---

## 1. Resumen de Actores Humanos Aprobados

| ID | Actor Humano | Responsabilidad Central | Ámbitos del MVP con los que interactúa | Estado |
| :--- | :--- | :--- | :--- | :--- |
| **ACT-01** | **Operador de recepción** | Registrar emergencias y consultar información y estados de las solicitudes. | Gestión de emergencias, Seguimiento simulado (consulta). | **Aprobado** (`DEC-009`) |
| **ACT-02** | **Despachador** | Revisar emergencias, gestionar asignaciones y supervisar despachos. | Gestión de emergencias (revisión/cola), Asignación supervisada y automática, Rutas y seguimiento simulado. | **Aprobado** (`DEC-009`) |
| **ACT-03** | **Administrador** | Gestionar ambulancias, hospitales, reglas de clasificación y parámetros del simulador; configurar y ejecutar escenarios experimentales y consultar métricas. | Gestión de ambulancias, Gestión básica de hospitales, Reglas de triage, Simulación concurrente, Métricas experimentales. | **Aprobado** (`DEC-009`) |

---

## 2. Detalle de Actores Humanos

### 2.1 `ACT-01` — Operador de recepción

- **Identificador:** `ACT-01`
- **Naturaleza:** Actor humano operativo (rol académico simulado).
- **Descripción:** Representa al personal encargado del primer contacto en el ciclo de atención simulado. Introduce en el sistema los datos de cada emergencia médica entrante para que el simulador determine de manera reproducible su clasificación y requerimientos de equipamiento, y realiza el seguimiento informativo del estado de las solicitudes.
- **Responsabilidades aprobadas (`DEC-009`, `DEC-016`):**
  1. **Registrar emergencias:** Ingresar los campos de entrada aprobados para una nueva solicitud de emergencia: **categoría del incidente** (obligatoria; catálogo inicial `INC-A` a `INC-D`), **nodo de ubicación en el grafo** simplificado de Arequipa (obligatorio) y **descripción ficticia** (opcional), a partir de los cuales el sistema genera el identificador único y la marca temporal de registro y determina automáticamente, según la versión activa de reglas, la prioridad simulada informativa, el equipamiento requerido (`BASIC` o `SPECIALIZED`) y la referencia a la versión de reglas aplicada (`DEC-010`, `DEC-016`).
  2. **Consultar información y estados:** Visualizar el detalle de las emergencias registradas, la prioridad simulada informativa, el equipamiento requerido, la versión de reglas aplicada y el estado actual de cada solicitud a lo largo del ciclo simulado.
- **Límites de responsabilidad:**
  - No realiza diagnósticos ni toma decisiones clínicas reales (`DEC-001`, `DEC-010`, `DEC-016`).
  - No introduce manualmente la prioridad simulada ni el equipamiento requerido (son determinados por la versión activa de reglas configurables según la categoría ingresada), ni puede registrar emergencias sin categoría, con categorías inexistentes/inactivas o con nodos de ubicación inválidos (`DEC-016`).
  - No ejecuta la selección ni asignación de ambulancias (responsabilidad del `Despachador` o del modo automático); si al registrar una emergencia válida no existen ambulancias elegibles, el registro se conserva y la emergencia pasa a estado pendiente (`DEC-011`, `DEC-016`).
  - No administra el catálogo de ambulancias, hospitales, reglas de triage ni escenarios experimentales (responsabilidad del `Administrador`).

---

### 2.2 `ACT-02` — Despachador

- **Identificador:** `ACT-02`
- **Naturaleza:** Actor humano operativo (rol académico simulado).
- **Descripción:** Representa al responsable de coordinar la respuesta operativa ante las emergencias registradas y pendientes, tomando decisiones de asignación en modalidad supervisada y monitoreando el progreso de los despachos y de las unidades hasta su llegada al hospital.
- **Responsabilidades aprobadas (`DEC-009`, `DEC-011`):**
  1. **Revisar emergencias:** Consultar las emergencias activas y la cola de emergencias pendientes junto con su clasificación simulada y equipamiento requerido.
  2. **Gestionar asignaciones:**
     - En **modalidad supervisada**, revisar la lista de ambulancias elegibles y la recomendación calculada por costo de desplazamiento, pudiendo seleccionar **cualquier ambulancia elegible**, aunque no sea la primera recomendada (`DEC-011`).
     - Si la ambulancia seleccionada deja de estar disponible debido a una operación concurrente antes de confirmarse la asignación, recibir la lista actualizada de alternativas elegibles y efectuar una nueva selección (`DEC-011`).
     - Observar el resultado de las asignaciones ejecutadas en **modalidad automática**.
  3. **Supervisar despachos:** Hacer seguimiento del estado operativo de las asignaciones, del desplazamiento simulado de las ambulancias por la ruta calculada y del arribo al hospital (`DEC-007`).
- **Límites de responsabilidad:**
  - No puede asignar una ambulancia que no cumpla las condiciones previas de elegibilidad (disponibilidad, estado y equipamiento requerido) ni violar la regla de preservación de ambulancias especializadas (`DEC-011`).
  - No administra el catálogo estructural de ambulancias/hospitales ni modifica las reglas configurables de clasificación (`DEC-009`).
  - No configura ni ejecuta escenarios experimentales de simulación masiva (responsabilidad exclusiva del `Administrador`).

---

### 2.3 `ACT-03` — Administrador

- **Identificador:** `ACT-03`
- **Naturaleza:** Actor humano de configuración, administración y experimentación académica.
- **Descripción:** Representa al responsable de configurar los recursos base, las reglas y parámetros del simulador, así como de diseñar, ejecutar y evaluar los experimentos académicos de concurrencia y desempeño.
- **Responsabilidades aprobadas (`DEC-009`, `DEC-010`, `DEC-012`, `DEC-016`):**
  1. **Gestionar ambulancias:** Registrar y administrar las ambulancias del simulador (básicas `BASIC` y especializadas `SPECIALIZED`), sus características de equipamiento, ubicación inicial y estado operativo.
  2. **Gestionar hospitales:** Registrar y administrar de forma básica los hospitales del simulador, su ubicación en la red simplificada y su disponibilidad simulada (`DEC-014`).
  3. **Gestionar reglas de clasificación (triage):** Configurar y publicar versiones inmutables de reglas de clasificación —manteniendo la trazabilidad de la versión aplicada en cada emergencia según `DEC-010` y `DEC-016`— mediante las cuales el sistema determina de manera determinista la prioridad simulada informativa y el equipamiento requerido (`BASIC` o `SPECIALIZED`) para cada categoría de incidente, sin modificar retroactivamente emergencias ya clasificadas.
  4. **Gestionar parámetros del simulador:** Configurar los parámetros operativos del sistema (como la modalidad de asignación o los parámetros de control pendientes de definición).
  5. **Configurar y ejecutar escenarios experimentales:** Definir y disparar escenarios de simulación concurrente para estudiar el comportamiento del sistema ante solicitudes simultáneas y competencia por recursos (`DEC-008`, `DEC-009`).
  6. **Consultar métricas:** Inspeccionar los resultados y métricas generadas por la operación y los escenarios experimentales, incluyendo tiempos de espera en cola y evidencia sobre posibles situaciones de *starvation* (`DEC-008`, `DEC-012`).
- **Límites de responsabilidad:**
  - Las reglas de clasificación configuradas son estrictamente académicas y simuladas; no constituyen protocolos clínicos reales (`DEC-001`, `DEC-010`, `DEC-016`).
  - No puede modificar versiones de reglas ya publicadas (son inmutables), ni publicar versiones con categorías duplicadas, incompletas o con reglas contradictorias (`DEC-016`).
  - **No puede publicar ni activar cambios de reglas de clasificación durante la ejecución de un escenario experimental activo (`DEC-016`, `RN-EME-012`).**

---

## 3. Componentes Internos del Sistema (No Actores Humanos)

### 3.1 `CMP-INT-01` — Motor de simulación

- **Identificador:** `CMP-INT-01`
- **Naturaleza:** Componente interno de software (**no es un actor humano** — `DEC-009`).
- **Propósito dentro de la arquitectura:** Ejecutar internamente la dinámica temporal y concurrente del simulador a partir de los escenarios e instrucciones iniciados por el `Administrador` o de los despachos en curso:
  - Generación o procesamiento concurrente de solicitudes de emergencia definidas en un escenario experimental.
  - Ejecución de la evaluación y reintentos acotados en modalidad de asignación automática (`DEC-011`).
  - Avance del desplazamiento simulado de las ambulancias a través de las rutas calculadas con Dijkstra hasta la llegada al hospital (`DEC-007`, `DEC-013`).
  - Recolección de marcas de tiempo y métricas de espera/despacho para su posterior consulta por el `Administrador` (`DEC-012`).

---

## 4. Matriz de Responsabilidades por Capacidad del MVP

| Capacidad del MVP (`DEC-008`) | Operador de recepción (`ACT-01`) | Despachador (`ACT-02`) | Administrador (`ACT-03`) | Sistema / Motor Interno (`CMP-INT-01`) |
| :--- | :---: | :---: | :---: | :---: |
| **Registro de emergencias** | Ejecuta | Consulta / Revisa | Configura vía escenarios | Genera en escenarios experimentales |
| **Consulta de información y estados de emergencias** | Ejecuta | Ejecuta | Consulta en métricas | Actualiza estados |
| **Configuración de reglas de clasificación (triage)** | — | — | Ejecuta | Evalúa reglas de forma reproducible y trazable |
| **Gestión de ambulancias (catálogo y recursos)** | — | Consulta elegibilidad/estado | Ejecuta | Actualiza disponibilidad y ubicación simulada |
| **Gestión básica de hospitales** | — | Consulta | Ejecuta | Actualiza disponibilidad simulada |
| **Asignación supervisada** | — | Selecciona unidad elegible / Resuelve recálculo | Configura parámetros | Filtra elegibles, recomienda por costo y valida concurrencia |
| **Asignación automática** | — | Supervisa | Configura parámetros | Selecciona, asigna y ejecuta reintentos limitados |
| **Cálculo de rutas (Dijkstra) y seguimiento simulado** | Consulta estado | Supervisa desplazamiento | Configura parámetros | Calcula ruta con Dijkstra y avanza desplazamiento hasta hospital |
| **Simulación concurrente y métricas experimentales** | — | — | Configura, ejecuta y consulta métricas | Ejecuta concurrencia y registra tiempos de espera / *starvation* |

---

## 5. Aspectos de Actores Pendientes de Definición

Los siguientes detalles no han sido cerrados todavía y se mantienen como **pendientes de definición** antes del diseño de la capa de presentación:

1. **Mecanismo de acceso y cambio de rol en la interfaz web:** Definir si los tres actores humanos (`Operador de recepción`, `Despachador` y `Administrador`) operarán mediante vistas/consolas separadas con un selector académico de rol o si se requerirá algún esquema de inicio de sesión.
2. **Edición de otros catálogos y preparación de borradores de reglas durante una simulación activa:** Aunque `DEC-016` (`RN-EME-012`) ya aprobó que **no se permite publicar ni activar cambios en las reglas de clasificación durante la ejecución de un escenario experimental activo**, permanece pendiente de definición:
   - Si el `Administrador` podrá o no preparar **borradores** de reglas de clasificación durante un escenario experimental activo (`DEC-016`).
   - Si el `Administrador` podrá modificar los demás catálogos (`ambulancias`, `hospitales`) mientras un escenario experimental está en ejecución o únicamente antes/después de cada ejecución.

# Reglas de Negocio — ARE (Arequipa RescUE)

**Estado del documento:** Aprobado — Línea base aceptada (`DEC-015`) y formalización de reglas de clasificación y equipamiento de `PEN-002a` (`DEC-016`), conservando `RN-ASI-005` y `RN-MET-003` como propuestas pendientes y los parámetros pendientes de `PEN-002b` a `PEN-002d`
**Propósito:** Formalizar las reglas de negocio e invariantes de dominio de **ARE – Arequipa RescUE** con identificadores estables, distinguiendo con exactitud qué reglas están **Aprobadas** por el responsable del proyecto y qué propuestas, parámetros o reglas específicas permanecen **Pendientes de aprobación o definición**.

> [!IMPORTANT]
> Ningún parámetro pendiente (como el número máximo de reintentos, las reglas de selección de hospital o la preparación de borradores durante escenarios activos) se convierte en una regla definitiva en este documento. Todo elemento no cerrado se declara expresamente como **Pendiente de definición**.

---

## 1. Resumen de Categorías de Reglas

| Prefijo | Tema | Reglas Aprobadas | Reglas / Parámetros Pendientes |
| :--- | :--- | :---: | :---: |
| **`RN-EME`** | 1. Registro y clasificación de emergencias | `RN-EME-001` a `RN-EME-012` | Pendiente: preparación de borradores en escenarios activos (`DEC-016`, `stakeholders.md` Secc. 5.2) |
| **`RN-ELE`** | 2. Elegibilidad y recomendación de ambulancias | `RN-ELE-001` a `RN-ELE-004` | — |
| **`RN-ASI`** | 3. Asignación supervisada y automática | `RN-ASI-001` a `RN-ASI-004` | `RN-ASI-005` (propuesta pendiente) |
| **`RN-PRE`** | 4. Preservación de ambulancias especializadas | `RN-PRE-001` a `RN-PRE-003` | — |
| **`RN-CON`** | 5. Conflictos concurrentes y reintentos | `RN-CON-001` a `RN-CON-004` | `RN-CON-005` |
| **`RN-COL`** | 6. Cola de emergencias y FIFO | `RN-COL-001` a `RN-COL-006` | Pendiente: liberación de ambulancia básica (`PEN-002d`) |
| **`RN-RUT`** | 7. Rutas y seguimiento | `RN-RUT-001` a `RN-RUT-005` | `RN-RUT-006` |
| **`RN-HOS`** | 8. Hospitales | `RN-HOS-001` a `RN-HOS-003` | `RN-HOS-004` |
| **`RN-MET`** | 9. Métricas y experimentación | `RN-MET-001` a `RN-MET-002` | `RN-MET-003` (propuesta pendiente), `RN-MET-004` |

---

## 2. Catálogo de Reglas de Negocio por Tema

### Tema 1: Registro y clasificación de emergencias (`RN-EME`)

#### `RN-EME-001` — Inicio y fin del ciclo de una emergencia
- **Enunciado:** El ciclo de vida simulado de toda emergencia médica inicia con el registro de la solicitud y concluye cuando la ambulancia asignada llega al hospital de destino. No se simulan procesos posteriores de hospitalización, tratamiento ni atención médica real.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-001`, `DEC-007`, `docs/product/vision-and-scope.md` (Sección 7).

#### `RN-EME-002` — Clasificación mediante reglas configurables
- **Enunciado:** Al registrarse una emergencia, el sistema determina su clasificación simulada y los requisitos de equipamiento para su atención evaluando los datos registrados contra un conjunto de reglas configurables administradas en el sistema.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-010`, `docs/product/stakeholders.md` (`ACT-03`).

#### `RN-EME-003` — Reproducibilidad de la clasificación (Determinismo)
- **Enunciado:** La evaluación de triage debe ser estrictamente reproducible: dados exactamente los mismos datos de entrada de una emergencia y la misma versión de reglas de clasificación, el sistema debe producir siempre el mismo resultado de clasificación y de equipamiento requerido.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-010`.

#### `RN-EME-004` — Trazabilidad de la regla y configuración aplicada
- **Enunciado:** Toda emergencia clasificada debe conservar trazabilidad sobre qué reglas o versión de configuración se aplicaron para obtener su clasificación simulada y sus requisitos de equipamiento.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-010`.

#### `RN-EME-005` — Carácter no clínico de la clasificación
- **Enunciado:** La clasificación obtenida mediante las reglas configurables es una abstracción académica para la asignación de recursos en el simulador; no representa un diagnóstico médico ni una decisión clínica real.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-001`, `DEC-010`.

#### `RN-EME-006` — Responsabilidad de registro y consulta
- **Enunciado:** El `Operador de recepción` (`ACT-01`) está facultado para registrar emergencias y consultar su información y estados; el motor interno de simulación (`CMP-INT-01`) también puede generar solicitudes de emergencia durante la ejecución de escenarios experimentales configurados por el `Administrador` (`ACT-03`).
- **Estado:** **Aprobada**
- **Referencias:** `DEC-009`, `docs/product/stakeholders.md`.

#### `RN-EME-007` — Catálogo inicial de categorías de incidente, prioridad simulada y equipamiento requerido
- **Enunciado:** El sistema utiliza un modelo determinista de clasificación mediante categorías ficticias y reglas configurables (sin algoritmos de diagnóstico clínico ni criterios médicos reales), consultando la versión activa de reglas para determinar la prioridad simulada informativa y la categoría de equipamiento requerido conforme al siguiente catálogo inicial exclusivamente académico:

  | Categoría | Prioridad simulada | Equipamiento requerido |
  | :--- | :--- | :--- |
  | `INC-A` | Alta | Especializado (`SPECIALIZED`) |
  | `INC-B` | Media | Básico (`BASIC`) |
  | `INC-C` | Baja | Básico (`BASIC`) |
  | `INC-D` | Alta | Básico (`BASIC`) |

  Se establecen únicamente dos categorías iniciales de equipamiento sin crear categorías adicionales:
  - `BASIC`: equipamiento básico.
  - `SPECIALIZED`: equipamiento especializado, compatible también con necesidades básicas.
  Una ambulancia básica (`BASIC`) no puede satisfacer una necesidad especializada (`SPECIALIZED`), mientras que una ambulancia especializada (`SPECIALIZED`) puede satisfacer una necesidad básica (`BASIC`) respetando las reglas aprobadas de preservación (`RN-PRE-001` a `RN-PRE-003`).
- **Estado:** **Aprobada** (cierra `PEN-002a`)
- **Referencias:** `DEC-001`, `DEC-010`, `DEC-011`, `DEC-015`, `DEC-016`, `RN-PRE-001` a `RN-PRE-003`.

#### `RN-EME-008` — Carácter exclusivamente informativo de la prioridad simulada
- **Enunciado:** La prioridad simulada (`Alta`, `Media`, `Baja`) asignada a una emergencia es únicamente informativa. No altera:
  1. El orden FIFO aprobado para las colas (`RN-COL-003`).
  2. La preservación de ambulancias especializadas (`RN-PRE-001` a `RN-PRE-003`).
  3. Las condiciones de elegibilidad (`RN-ELE-002`).
  4. El ordenamiento por costo de ruta mediante Dijkstra (`RN-ELE-003`, `RN-RUT-002`).
  El equipamiento requerido (`BASIC` o `SPECIALIZED`) es el atributo que determina la compatibilidad de las ambulancias.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-010`, `DEC-011`, `DEC-012`, `DEC-013`, `DEC-016`.

#### `RN-EME-009` — Campos del registro de emergencia (introducidos y determinados automáticamente)
- **Enunciado:** Todo registro de emergencia comprende:
  - **Campos introducidos por el `Operador de recepción` (`ACT-01`):**
    1. Categoría del incidente: obligatoria.
    2. Nodo de ubicación en el grafo: obligatorio.
    3. Descripción ficticia: opcional (se permite descripción vacía).
  - **Campos generados o determinados automáticamente por el sistema:**
    1. Identificador único de emergencia.
    2. Marca temporal de registro.
    3. Prioridad simulada.
    4. Equipamiento requerido (`BASIC` o `SPECIALIZED`).
    5. Identificador o referencia de la versión de reglas aplicada.
  *(Nota: el formato físico de los identificadores, tipos SQL, precisión temporal y catálogo concreto de nodos del grafo se definirán en las decisiones pendientes correspondientes).*
- **Estado:** **Aprobada**
- **Referencias:** `DEC-009`, `DEC-010`, `DEC-016`, `docs/product/stakeholders.md` (`ACT-01`).

#### `RN-EME-010` — Validaciones del registro de emergencia e independencia frente a falta de ambulancias
- **Enunciado:** Al registrar una emergencia, el sistema debe:
  1. Rechazar registros sin categoría.
  2. Rechazar categorías inexistentes o inactivas.
  3. Rechazar ubicaciones que no correspondan a nodos válidos del grafo configurado.
  4. Permitir que la descripción ficticia esté vacía.
  5. Exigir una regla válida y no ambigua en la versión activa para clasificar la categoría, sin generar clasificaciones por defecto cuando falte una regla válida.
  6. Mantener la referencia histórica a la versión de reglas aplicada.
  7. Si una vez registrada y clasificada la emergencia no existen ambulancias elegibles, conservar la emergencia registrada y pasarla a estado **pendiente** según las reglas aprobadas (`RN-ASI-004`). Una falla de asignación por falta de ambulancias no equivale a una falla del registro de emergencia.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-010`, `DEC-011`, `DEC-016`, `RN-ASI-004`.

#### `RN-EME-011` — Versionado inmutable de reglas de clasificación y consistencia de asociación
- **Enunciado:** Las reglas de clasificación son configurables y versionadas bajo las siguientes invariantes:
  1. Existirá una única versión activa de reglas aplicable a los nuevos registros.
  2. Cada emergencia conservará la referencia histórica a la versión que produjo su clasificación.
  3. Las versiones publicadas son inmutables; cualquier cambio debe incorporarse mediante la creación y publicación de una nueva versión.
  4. Publicar una nueva versión no modifica retroactivamente las emergencias ya clasificadas.
  5. Se debe impedir la publicación de versiones con categorías duplicadas, incompletas o con reglas contradictorias.
  6. Para una misma categoría y versión de reglas, el resultado de clasificación debe ser determinista (`RN-EME-003`), y cada emergencia debe quedar asociada a una única versión consistente incluso cuando existan operaciones concurrentes. *(La estrategia técnica de persistencia, bloqueo o activación atómica se definirá posteriormente en arquitectura y persistencia).*
- **Estado:** **Aprobada**
- **Referencias:** `DEC-010`, `DEC-016`, `RN-EME-003`, `RN-EME-004`.

#### `RN-EME-012` — Bloqueo de publicación y activación de reglas durante escenarios experimentales activos
- **Enunciado:** Durante la ejecución de un escenario experimental activo no se permite publicar ni activar cambios en las reglas de clasificación.
- **Estado:** **Aprobada**
- **Nota de alcance pendiente:** La preparación de borradores de reglas durante escenarios activos no queda aprobada ni prohibida por `DEC-016` y permanece **Pendiente de definición** (`stakeholders.md` Sección 5.2).
- **Referencias:** `DEC-010`, `DEC-016`, `docs/product/stakeholders.md` (`ACT-03`, Sección 5.2).

---

### Tema 2: Elegibilidad y recomendación de ambulancias (`RN-ELE`)

#### `RN-ELE-001` — Evaluación de elegibilidad previa a la recomendación
- **Enunciado:** El sistema debe determinar el subconjunto de ambulancias elegibles **antes** de calcular o presentar cualquier recomendación de asignación. Ninguna ambulancia no elegible puede ser recomendada ni asignada.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-011`.

#### `RN-ELE-002` — Criterios obligatorios de elegibilidad
- **Enunciado:** Para que una ambulancia sea considerada elegible frente a una emergencia, debe cumplir simultáneamente con los criterios de:
  1. **Disponibilidad** actual.
  2. **Estado operativo** apto para despacho.
  3. **Equipamiento requerido** compatible con las necesidades determinadas para la emergencia.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-011`.

#### `RN-ELE-003` — Comparación por costo de desplazamiento entre elegibles
- **Enunciado:** Entre las ambulancias que resulten elegibles (respetando las reglas de preservación `RN-PRE`), el sistema calcula y compara el costo de desplazamiento hacia la ubicación de la emergencia para ordenar las recomendaciones o seleccionar la unidad óptima.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-011`, `DEC-013`.

#### `RN-ELE-004` — Independencia respecto a la disponibilidad hospitalaria en la selección inicial
- **Enunciado:** La disponibilidad hospitalaria no interviene como criterio de filtrado ni como restricción en la selección inicial de ambulancias para atender una emergencia (regla canónica del acuerdo `DEC-014`, vinculada con `RN-HOS-002`).
- **Estado:** **Aprobada**
- **Referencias:** `DEC-014`, `RN-HOS-002`.

---

### Tema 3: Asignación supervisada y automática (`RN-ASI`)

#### `RN-ASI-001` — Modalidades de asignación soportadas
- **Enunciado:** El sistema debe soportar dos modalidades de asignación de ambulancias: **modalidad supervisada** (con intervención del `Despachador`) y **modalidad automática** (ejecutada directamente por el sistema sin confirmación humana).
- **Estado:** **Aprobada**
- **Referencias:** `DEC-008`, `DEC-011`.

#### `RN-ASI-002` — Libertad de selección dentro de las elegibles en modalidad supervisada
- **Enunciado:** En modalidad supervisada, el sistema presenta las ambulancias elegibles junto con su recomendación basada en costo de desplazamiento, pero el `Despachador` (`ACT-02`) puede seleccionar **cualquier ambulancia elegible**, aunque no sea la primera recomendada.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-011`, `docs/product/stakeholders.md` (`ACT-02`).

#### `RN-ASI-003` — Selección y asignación directa en modalidad automática
- **Enunciado:** En modalidad automática, una vez determinadas las ambulancias elegibles y evaluado el criterio de preservación y menor costo de desplazamiento, el sistema selecciona y asigna la ambulancia de forma inmediata sin requerir confirmación humana.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-011`.

#### `RN-ASI-004` — Permanencia en estado pendiente ante falta de ambulancias elegibles
- **Enunciado:** Si al evaluar una emergencia (en cualquiera de las dos modalidades) no existen ambulancias elegibles disponibles, no se realiza asignación alguna y la emergencia permanece en estado **pendiente** dentro de la cola de espera.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-011`.

#### `RN-ASI-005` — Desempate ante igualdad exacta de elegibilidad, tipo y costo de desplazamiento
- **Enunciado:** Cuando dos o más ambulancias elegibles del mismo grupo presenten exactamente el mismo costo de desplazamiento mínimo en modalidad automática, deberá definirse si se aplica una regla de desempate y bajo qué criterio.
- **Estado:** **Propuesta pendiente de aprobación / Pendiente de definición** (`PEN-002d`)
- **Decisión faltante:** Aprobar si existirá una regla específica de desempate en modalidad automática y definir su criterio concreto.

---

### Tema 4: Preservación de ambulancias especializadas (`RN-PRE`)

#### `RN-PRE-001` — Preferencia de ambulancias básicas para emergencias básicas
- **Enunciado:** Cuando una emergencia pueda ser atendida con equipamiento básico, el sistema debe preferir las ambulancias básicas elegibles por encima de las ambulancias especializadas, con el fin de preservar los recursos especializados para incidentes que los requieran estrictamente.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-011`.

#### `RN-PRE-002` — Condición de uso excepcional de ambulancia especializada en emergencia básica
- **Enunciado:** Una ambulancia especializada puede utilizarse para atender una emergencia básica **únicamente si no existen alternativas básicas elegibles** en ese momento y la ambulancia especializada cumple con todos los requisitos de elegibilidad.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-011`.

#### `RN-PRE-003` — Restricción para emergencias que requieren equipamiento especializado
- **Enunciado:** Una emergencia cuyos requisitos de equipamiento exijan una ambulancia especializada solo puede considerar como elegibles a las ambulancias que posean dicho equipamiento especializado; las ambulancias básicas son no elegibles para ese incidente.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-011`, `DEC-012`.

---

### Tema 5: Conflictos concurrentes y reintentos (`RN-CON`)

#### `RN-CON-001` — Prevención de asignaciones simultáneas incompatibles
- **Enunciado:** Una ambulancia no puede ser asignada simultáneamente a más de una emergencia activa. Si dos operaciones concurrentes intentan asignar la misma ambulancia disponible, a lo sumo una de ellas puede concretar la asignación y la otra debe ser rechazada por pérdida de disponibilidad del recurso.
- **Estado:** **Aprobada**
- **Referencias:** `AGENTS.md` (Secciones 1.3, 6.1, 6.2), `DEC-011`.

#### `RN-CON-002` — Manejo de pérdida de disponibilidad en modalidad supervisada
- **Enunciado:** Si en modalidad supervisada la ambulancia seleccionada por el `Despachador` deja de estar disponible antes de concretarse la asignación (por haber sido asignada concurrentemente o cambiar su estado), el sistema cancela el intento sobre esa unidad, actualiza la lista de alternativas elegibles y solicita una nueva selección al `Despachador`.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-011`.

#### `RN-CON-003` — Reevaluación y reintento automático ante conflicto concurrente
- **Enunciado:** Si una asignación concurrente falla porque la ambulancia objetivo dejó de estar disponible, el sistema en modalidad automática reevalúa las alternativas restantes y reintenta automáticamente la asignación con otra ambulancia elegible.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-011`.

#### `RN-CON-004` — Límite de reintentos y retorno a estado pendiente
- **Enunciado:** Los reintentos automáticos ante fallos de asignación concurrente son limitados. Si se agota el límite máximo de reintentos o ya no quedan ambulancias elegibles durante la reevaluación, el proceso se detiene y la emergencia permanece en estado **pendiente**.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-011`.

#### `RN-CON-005` — Valor del número máximo de reintentos automáticos
- **Enunciado:** El parámetro numérico exacto que define el número máximo de reintentos automáticos ante fallos por concurrencia debe establecerse y aprobarse (como constante o parámetro configurable del simulador).
- **Estado:** **Pendiente de definición** (`PEN-002b`)
- **Decisión faltante:** Definir el valor exacto o rango configurable del número máximo de reintentos.

---

### Tema 6: Cola de emergencias y FIFO (`RN-COL`)

#### `RN-COL-001` — Priorización de grupo especializado al liberarse una ambulancia especializada
- **Enunciado:** Cuando una ambulancia especializada pasa a estar disponible y existen emergencias pendientes en la cola, el sistema considera **en primer lugar** al grupo de emergencias pendientes que requieren específicamente ese equipamiento especializado.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-012`.

#### `RN-COL-002` — Atención supletoria del grupo básico al liberarse una ambulancia especializada
- **Enunciado:** Si al liberarse una ambulancia especializada no existen emergencias pendientes que requieran específicamente ese equipamiento especializado, el sistema considera en segundo lugar al grupo de emergencias básicas pendientes que sean compatibles con dicha unidad.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-011`, `DEC-012`.

#### `RN-COL-003` — Orden FIFO estricto dentro de cada grupo de la cola
- **Enunciado:** Dentro de cada grupo de emergencias pendientes consideradas, el orden de atención sigue estrictamente la disciplina **FIFO (*First-In, First-Out*)** según el momento (marca temporal) de registro de la emergencia.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-012`.

#### `RN-COL-004` — Distinción entre requerimiento de equipamiento y prioridad clínica
- **Enunciado:** La agrupación por necesidad de equipamiento representa únicamente una condición técnica de compatibilidad y preservación de recursos del simulador; **no debe interpretarse ni documentarse como una prioridad clínica**.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-001`, `DEC-012`.

#### `RN-COL-005` — Ausencia de *aging* automático en la primera versión
- **Enunciado:** En la primera versión del simulador no se aplicará ningún mecanismo de envejecimiento (*aging*) automático que altere el grupo o la posición FIFO de las emergencias en espera por el paso del tiempo.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-012`.

#### `RN-COL-006` — Registro de tiempos de espera en cola para estudio de *starvation*
- **Enunciado:** El sistema debe registrar las marcas de tiempo y los tiempos de espera en cola de cada emergencia (como fuente de datos para las métricas de `RN-MET-002`) con el fin de observar posibles situaciones de inanición (*starvation*).
- **Estado:** **Aprobada**
- **Referencias:** `DEC-012`, `RN-MET-002`.

> **Asunto pendiente de definición en Tema 6 (`PEN-002d`):** Se encuentra pendiente de definir y aprobar el comportamiento de la cola de emergencias cuando se libera una ambulancia básica. No se asume ni inventa una política no aprobada para este caso.

---

### Tema 7: Rutas y seguimiento (`RN-RUT`)

#### `RN-RUT-001` — Representación geográfica mediante red vial simplificada de Arequipa
- **Enunciado:** Las ubicaciones de emergencias, ambulancias y hospitales, así como los trayectos entre ellas, se representan sobre un grafo simplificado de la red vial inspirado en Arequipa, con un nivel de realismo intermedio y sin depender de GPS ni tráfico reales.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-007`, `DEC-008`, `DEC-013`.

#### `RN-RUT-002` — Uso de Dijkstra como algoritmo inicial de cálculo de rutas
- **Enunciado:** El cálculo de rutas y del costo de desplazamiento en el MVP se realiza mediante el algoritmo de **Dijkstra** sobre el grafo vial simplificado.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-008`, `DEC-013`.

#### `RN-RUT-003` — Carácter opcional/experimental de A* fuera del MVP
- **Enunciado:** El algoritmo **A\*** no es un requisito obligatorio del MVP; se contempla únicamente como posible extensión futura o alternativa para comparación experimental.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-008`, `DEC-013`.

#### `RN-RUT-004` — Separación entre algoritmos de rutas y asignación concurrente
- **Enunciado:** La lógica algorítmica de cálculo de rutas sobre el grafo (Dijkstra) es independiente y no debe confundirse ni acoplarse con los mecanismos de asignación concurrente, bloqueo o control transaccional de recursos.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-013`.

#### `RN-RUT-005` — Desplazamiento simulado y cierre al llegar al hospital
- **Enunciado:** El sistema representa mediante desplazamientos simulados el avance de la ambulancia asignada hasta culminar con la llegada al hospital, hito que marca el fin del ciclo simulado para esa emergencia.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-007`, `DEC-008`.

#### `RN-RUT-006` — Topología del grafo y mecánica de avance temporal del desplazamiento
- **Enunciado:** La definición concreta de nodos, aristas y pesos del grafo simplificado de Arequipa, así como la regla exacta de avance temporal de los desplazamientos simulados y el listado de estados intermedios, deberán especificarse antes de su implementación.
- **Estado:** **Pendiente de definición** (`PEN-002d`)
- **Decisión faltante:** Definir los nodos/pesos del grafo, los estados intermedios del traslado y el ritmo de avance del reloj simulado.

---

### Tema 8: Hospitales (`RN-HOS`)

#### `RN-HOS-001` — Gestión básica de hospitales y disponibilidad simulada
- **Enunciado:** El simulador incluye una gestión básica de hospitales que representa su identificación, su ubicación dentro del grafo simplificado y su disponibilidad simulada.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-008`, `DEC-014`.

#### `RN-HOS-002` — Desacoplamiento hospitalario respecto al despacho inicial (Referencia a `RN-ELE-004`)
- **Enunciado:** Conforme a la regla canónica `RN-ELE-004`, el estado de disponibilidad simulada de los hospitales no condiciona ni bloquea la selección inicial de ambulancias (`DEC-014`).
- **Estado:** **Aprobada** (consolidada con `RN-ELE-004` para evitar duplicación normativa)
- **Referencias:** `DEC-014`, `RN-ELE-004`.

#### `RN-HOS-003` — Exclusión de procesos intrahospitalarios
- **Enunciado:** La responsabilidad del simulador respecto al hospital termina con la llegada de la ambulancia al establecimiento; no se simulan procesos de internamiento, hospitalización ni tratamiento médico.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-007`.

#### `RN-HOS-004` — Reglas de selección de hospital de destino y gestión de capacidad
- **Enunciado:** Las reglas específicas para seleccionar a qué hospital se dirige la ambulancia y cómo se actualiza o valida su capacidad/disponibilidad simulada permanecen pendientes de definición.
- **Estado:** **Pendiente de definición** (`PEN-002c`)
- **Decisión faltante:** Definir el momento y criterio de selección del hospital destino y el efecto de la llegada sobre su disponibilidad simulada.

---

### Tema 9: Métricas y experimentación (`RN-MET`)

#### `RN-MET-001` — Configuración y ejecución de escenarios experimentales por el Administrador
- **Enunciado:** El `Administrador` (`ACT-03`) puede configurar y ejecutar escenarios experimentales de simulación concurrente y consultar las métricas resultantes.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-008`, `DEC-009`, `docs/product/stakeholders.md` (`ACT-03`).

#### `RN-MET-002` — Consulta de métricas de espera y observación de *starvation* (Referencia a `RN-COL-006`)
- **Enunciado:** A partir de los tiempos de espera en cola registrados según `RN-COL-006`, el sistema expone métricas consultables por el `Administrador` para observar posibles situaciones de *starvation*.
- **Estado:** **Aprobada** (complementaria de `RN-COL-006` sin duplicar la regla de registro)
- **Referencias:** `DEC-009`, `DEC-012`, `RN-COL-006`.

#### `RN-MET-003` — Auditoría de resultados en escenarios experimentales
- **Enunciado:** Se propone que los resultados de un escenario experimental permitan consultar de forma consolidada qué versión de reglas de clasificación se aplicó (manteniendo la trazabilidad aprobada por emergencia en `DEC-010` y `RN-EME-004`) y el resumen de asignaciones y conflictos concurrentes ocurridos.
- **Estado:** **Propuesta pendiente de aprobación** (`PEN-002d`)
- **Decisión faltante:** Aprobar el contenido específico de auditoría que integrará el reporte de resultados de los escenarios experimentales junto con el catálogo de métricas de `RN-MET-004`.
- **Referencias:** `DEC-010`, `RN-EME-004`, `RN-MET-004`.

#### `RN-MET-004` — Catálogo detallado de indicadores y parámetros de escenarios experimentales
- **Enunciado:** El listado completo de fórmulas/indicadores estadísticos (promedios, máximos, conteo de colisiones/reintentos) y los parámetros configurables de entrada de un escenario experimental deberán detallarse en la especificación de requisitos funcionales.
- **Estado:** **Pendiente de definición** (`PEN-002d`)
- **Decisión faltante:** Aprobar el listado exacto de métricas e indicadores que mostrará el panel experimental del Administrador.

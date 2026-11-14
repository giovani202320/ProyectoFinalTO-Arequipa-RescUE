# Reglas de Negocio — ARE (Arequipa RescUE)

**Estado del documento:** Aprobado — Línea base aceptada (`DEC-015`) y formalizaciones de `PEN-002a` (`DEC-016`), `PEN-002b` (`DEC-017`) y `PEN-002c` (`DEC-018`), conservando `RN-ASI-005` y `RN-MET-003` como propuestas pendientes y los parámetros pendientes de `PEN-002d`
**Propósito:** Formalizar las reglas de negocio e invariantes de dominio de **ARE – Arequipa RescUE** con identificadores estables, distinguiendo con exactitud qué reglas están **Aprobadas** por el responsable del proyecto y qué propuestas, parámetros o reglas específicas permanecen **Pendientes de aprobación o definición**.

> [!IMPORTANT]
> Ningún parámetro pendiente (como la liberación de ambulancia básica, los criterios de desempate en asignación de ambulancias, los estados definitivos, la cancelación/reasignación de reservas hospitalarias confirmadas, los permisos exactos para el alta simulada o la preparación de borradores durante escenarios activos) se convierte en una regla definitiva en este documento. Todo elemento no cerrado se declara expresamente como **Pendiente de definición**.

---

## 1. Resumen de Categorías de Reglas

| Prefijo | Tema | Reglas Aprobadas | Reglas / Parámetros Pendientes |
| :--- | :--- | :---: | :---: |
| **`RN-EME`** | 1. Registro y clasificación de emergencias | `RN-EME-001` a `RN-EME-012` | Pendiente: preparación de borradores en escenarios activos (`DEC-016`, `stakeholders.md` Secc. 5.2) |
| **`RN-ELE`** | 2. Elegibilidad y recomendación de ambulancias | `RN-ELE-001` a `RN-ELE-004` | — |
| **`RN-ASI`** | 3. Asignación supervisada y automática | `RN-ASI-001` a `RN-ASI-004` | `RN-ASI-005` (propuesta pendiente) |
| **`RN-PRE`** | 4. Preservación de ambulancias especializadas | `RN-PRE-001` a `RN-PRE-003` | — |
| **`RN-CON`** | 5. Conflictos concurrentes y reintentos | `RN-CON-001` a `RN-CON-009` | — *(Mecanismos técnicos en `PEN-006`)* |
| **`RN-COL`** | 6. Cola de emergencias y FIFO | `RN-COL-001` a `RN-COL-006` | Pendiente: liberación de ambulancia básica (`PEN-002d`) |
| **`RN-RUT`** | 7. Rutas y seguimiento | `RN-RUT-001` a `RN-RUT-005` | `RN-RUT-006` |
| **`RN-HOS`** | 8. Hospitales | `RN-HOS-001` a `RN-HOS-015` | Pendientes diferidos en `PEN-002d`, `PEN-005`, `PEN-006` y `stakeholders.md` Secc. 5 (cancelación/reasignación de reservas, permisos de alta simulada, estados y liberación de ambulancia) |
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

#### `RN-CON-005` — Límite máximo de intentos en asignación automática y alcance exclusivo por conflicto concurrente
- **Enunciado:** Cada operación de **asignación automática** dispone de **1 intento inicial** y **hasta 3 reintentos automáticos adicionales**, lo que equivale a un **máximo de 4 intentos totales** dentro de una misma operación de asignación automática. Esta política se rige por las siguientes condiciones:
  1. Los reintentos adicionales solo se activan cuando un intento falla por un **conflicto concurrente de disponibilidad** (es decir, cuando otra operación concurrente modificó o comprometió la disponibilidad de la ambulancia seleccionada antes de que la asignación actual pudiera confirmarse).
  2. No se permiten reintentos ilimitados.
  3. Esta política **no aplica** ni debe confundirse con la ausencia normal de ambulancias elegibles ni con errores técnicos generales del sistema.
  4. **No se extiende** el límite de 3 reintentos automáticos a la modalidad de asignación supervisada (`RN-CON-002`).
- **Estado:** **Aprobada** (cierra `PEN-002b`)
- **Referencias:** `DEC-011`, `DEC-015`, `DEC-017`, `RN-CON-002`, `RN-CON-003`, `RN-CON-004`.

#### `RN-CON-006` — Reevaluación integral obligatoria y finalización anticipada en cada reintento
- **Enunciado:** Después de cada conflicto concurrente en modalidad automática, y antes de intentar una nueva asignación, el sistema debe obligatoriamente:
  1. Consultar nuevamente la disponibilidad de ambulancias.
  2. Reevaluar las condiciones de elegibilidad (`RN-ELE-002`).
  3. Respetar la compatibilidad de equipamiento (`BASIC` y `SPECIALIZED`, `RN-EME-007`).
  4. Aplicar las reglas de preservación de ambulancias especializadas (`RN-PRE-001` a `RN-PRE-003`).
  5. Recalcular la selección según el costo de ruta mediante Dijkstra y las reglas vigentes (`RN-ELE-003`, `RN-RUT-002`).
  No se permite reintentar a ciegas sobre una ambulancia que ya dejó de estar disponible. Asimismo, el proceso de reintentos termina de forma anticipada e inmediata cuando:
  - Se confirma una asignación válida (deteniendo cualquier reintento adicional), o
  - Tras una reevaluación ya no existen ambulancias elegibles disponibles; en este caso la emergencia permanece en estado **pendiente** y **no se siguen consumiendo reintentos vacíos**. Si desde el inicio no existen ambulancias elegibles, la emergencia pasa o permanece en estado **pendiente** y se consumen `0` reintentos de concurrencia.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-011`, `DEC-013`, `DEC-016`, `DEC-017`, `RN-EME-007`, `RN-ELE-001` a `RN-ELE-003`, `RN-PRE-001` a `RN-PRE-003`, `RN-RUT-002`.

#### `RN-CON-007` — Tratamiento del agotamiento del límite de reintentos y notificación interna al Despachador
- **Enunciado:** Si el intento inicial y los 3 reintentos automáticos adicionales fallan por conflictos concurrentes (alcanzando el máximo de 4 intentos fallidos en la operación):
  1. La emergencia permanece registrada y en estado **pendiente** (nunca se cancela ni se elimina por este motivo).
  2. Se registra como motivo del resultado el agotamiento de reintentos por conflicto concurrente.
  3. Se notifica al `Despachador` (`ACT-02`) mediante un aviso visible en la interfaz web de ARE, de modo que pueda identificar desde la interfaz: (a) la emergencia que quedó en estado `pendiente`, (b) el motivo de agotamiento de reintentos por concurrencia y (c) que la operación automática terminó sin asignación.
  4. No se ejecutan más reintentos dentro de esa misma operación y no se introducen servicios externos de notificación (como correo electrónico o SMS).
  *(Nota: no se fija en esta etapa el componente visual específico, el mecanismo de refresco de pantalla ni la tecnología de notificación interna).*
- **Estado:** **Aprobada**
- **Referencias:** `DEC-009`, `DEC-011`, `DEC-017`, `docs/product/stakeholders.md` (`ACT-02`).

#### `RN-CON-008` — Registro funcional del resultado ante reintentos o fallo por conflicto concurrente
- **Enunciado:** Cuando una operación de asignación automática requiera reintentos o termine sin asignación por conflicto concurrente, el sistema debe conservar al menos la siguiente información funcional:
  1. Identificador de la emergencia.
  2. Resultado de la operación.
  3. Motivo del resultado, distinguiendo explícitamente el agotamiento de reintentos por concurrencia (por ejemplo, código funcional `CONCURRENCY_RETRY_EXHAUSTED`) de la falta de ambulancias elegibles y de errores técnicos generales.
  4. Número de intentos ejecutados.
  5. Número de reintentos adicionales consumidos.
  6. Tipo de operación (`automática`).
  7. Marca temporal del evento.
  *(Nota: no se fija en esta etapa el esquema SQL ni la estructura técnica definitiva de persistencia).*
- **Estado:** **Aprobada**
- **Referencias:** `DEC-011`, `DEC-017`.

#### `RN-CON-009` — Atomicidad funcional, preservación de invariantes e independencia del contador por operación
- **Enunciado:** Durante la resolución de conflictos concurrentes y la ejecución de reintentos automáticos deben respetarse las siguientes invariantes de consistencia:
  1. Ningún intento fallido puede dejar asignaciones parciales confirmadas, ni una ambulancia puede quedar asignada simultáneamente a dos emergencias incompatibles (`RN-CON-001`).
  2. Los reintentos no alteran el orden FIFO de las colas (`RN-COL-003`), las condiciones de elegibilidad (`RN-ELE-002`), la preservación de ambulancias especializadas (`RN-PRE-001` a `RN-PRE-003`) ni el cálculo de rutas mediante Dijkstra (`RN-RUT-002`).
  3. Cada operación de asignación independiente maneja su propio contador de intentos (hasta 1 intento inicial + 3 reintentos adicionales).
  4. Si posteriormente se inicia una nueva operación independiente de asignación sobre una emergencia que permanecía en estado `pendiente` (por ejemplo, cuando una ambulancia vuelve a estar disponible o se ejecuta un nuevo ciclo de atención de cola), esa nueva operación inicia su propio contador desde cero.
  *(Nota: los mecanismos técnicos de bloqueo optimista/pesimista, niveles de aislamiento, rollback, hilos/ejecutores y pruebas concurrentes se definirán en `PEN-006`).*
- **Estado:** **Aprobada**
- **Referencias:** `DEC-011`, `DEC-012`, `DEC-013`, `DEC-017`, `PEN-006`.

#### Casos límite funcionales aprobados para conflictos concurrentes y reintentos (`DEC-017`)

1. **Sin ambulancias elegibles en el intento inicial:** la emergencia queda en estado `pendiente` y se consumen `0` reintentos de concurrencia.
2. **Éxito en el intento inicial:** se confirma la asignación y se consumen `0` reintentos adicionales.
3. **Conflicto en el intento inicial y éxito en el primer reintento:** se confirma la nueva ambulancia elegible y se registra `1` reintento consumido.
4. **Conflicto en el intento inicial y ausencia de elegibles en la reevaluación:** la emergencia queda `pendiente` por falta de ambulancias elegibles sin seguir consumiendo reintentos.
5. **Agotamiento del intento inicial y de los 3 reintentos adicionales por conflictos concurrentes:** la emergencia queda `pendiente`, se registra el motivo de agotamiento de reintentos por concurrencia y se emite aviso visible al `Despachador` en la interfaz del sistema.
6. **Preservación de ambulancias especializadas durante reintentos:** para una emergencia `BASIC`, un reintento sigue prefiriendo ambulancias `BASIC` elegibles; solo puede usar una `SPECIALIZED` si no existen `BASIC` elegibles.
7. **Incompatibilidad de equipamiento durante reintentos:** para una emergencia `SPECIALIZED`, ningún reintento puede seleccionar una ambulancia `BASIC`; si solo quedan `BASIC`, la emergencia pasa a `pendiente` sin consumir reintentos adicionales.
8. **Conflicto en asignación supervisada:** si la ambulancia elegida manualmente por el `Despachador` pierde disponibilidad antes de confirmar, no se confirma la asignación, se actualizan las alternativas elegibles y se solicita una nueva selección manual.
9. **Nueva operación posterior:** si una emergencia quedó `pendiente` tras agotar reintentos y más adelante se dispara una nueva operación independiente de asignación (por ejemplo, cuando una ambulancia pasa a estar disponible), esa nueva operación inicia su propio contador desde cero.
10. **Error técnico distinto de conflicto concurrente:** un fallo técnico general no debe tratarse ni registrarse como agotamiento de reintentos de concurrencia.

#### Criterios de aceptación funcionales aprobados (`CA-CON-01` a `CA-CON-10`)

- **`CA-CON-01`:** Si la ambulancia seleccionada en asignación automática sigue disponible al confirmar, la asignación se completa en el intento inicial con `0` reintentos.
- **`CA-CON-02`:** Si el intento inicial falla por conflicto concurrente y existe otra ambulancia elegible, el sistema reevalúa y confirma la asignación en un reintento válido dentro del límite.
- **`CA-CON-03`:** En cada reintento se recalculan disponibilidad, elegibilidad, compatibilidad de equipamiento, preservación de especializadas y costo por Dijkstra.
- **`CA-CON-04`:** Si no existen ambulancias elegibles en el intento inicial, la emergencia queda `pendiente` con `0` reintentos de concurrencia.
- **`CA-CON-05`:** Si tras un conflicto concurrente la reevaluación determina que ya no quedan ambulancias elegibles, el proceso termina inmediatamente y la emergencia queda `pendiente` sin consumir reintentos restantes.
- **`CA-CON-06`:** Si fallan el intento inicial y los 3 reintentos adicionales por conflictos concurrentes, la emergencia queda `pendiente`, se registra el motivo de agotamiento de reintentos por concurrencia y se notifica al `Despachador` en la interfaz del sistema.
- **`CA-CON-07`:** Ante operaciones concurrentes que compiten por la misma ambulancia, nunca se producen dos asignaciones activas simultáneas sobre la misma unidad.
- **`CA-CON-08`:** Durante los reintentos para una emergencia `BASIC`, el sistema mantiene la preferencia por ambulancias `BASIC` y solo usa `SPECIALIZED` cuando no existen `BASIC` elegibles.
- **`CA-CON-09`:** Durante los reintentos para una emergencia `SPECIALIZED`, el sistema nunca asigna una ambulancia `BASIC`.
- **`CA-CON-10`:** En asignación supervisada, si la ambulancia elegida por el `Despachador` deja de estar disponible antes de confirmar, el sistema rechaza esa confirmación, actualiza las alternativas elegibles y solicita una nueva selección al `Despachador`.

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

#### `RN-HOS-004` — Capacidad hospitalaria numérica simulada e invariantes (`C`, `R`, `O`, `D`)
- **Enunciado:** Cada hospital cuenta con cuatro magnitudes enteras no negativas que representan plazas ficticias de simulación (no camas clínicas reales):
  - Capacidad total `C`.
  - Plazas reservadas `R`.
  - Plazas ocupadas `O`.
  - Plazas disponibles `D = C - R - O`.
  Deben cumplirse en todo momento las siguientes invariantes:
  1. `R + O <= C` (no puede existir sobreocupación).
  2. Una plaza no puede reservarse simultáneamente para dos emergencias.
  3. Una emergencia no puede mantener dos reservas hospitalarias activas.
  4. Una reserva y una ocupación no deben contabilizarse simultáneamente para la misma plaza y emergencia.
- **Estado:** **Aprobada** (cierra `PEN-002c`)
- **Referencias:** `DEC-001`, `DEC-007`, `DEC-014`, `DEC-018`.

#### `RN-HOS-005` — Elegibilidad hospitalaria y selección automática por menor costo de ruta (Dijkstra)
- **Enunciado:** La selección del hospital de destino comienza **al confirmar la recogida de la emergencia por la ambulancia** (sin intervenir en la selección inicial de ambulancias según `RN-ELE-004` y `RN-HOS-002`). Solo son elegibles los hospitales que cumplan simultáneamente:
  1. Estar **habilitados**.
  2. Ser **alcanzables** mediante el grafo simulado desde el nodo del incidente.
  3. Contar con **al menos una plaza disponible** (`D >= 1`).
  ARE calcula el costo de ruta desde el nodo del incidente hasta cada hospital elegible utilizando el algoritmo de **Dijkstra** (`RN-RUT-002`) y selecciona el hospital elegible con el **menor costo de ruta**.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-013`, `DEC-014`, `DEC-018`, `RN-ELE-004`, `RN-HOS-002`, `RN-RUT-002`.

#### `RN-HOS-006` — Desempate determinista ante igualdad de costo de ruta entre hospitales elegibles
- **Enunciado:** Si dos o más hospitales elegibles presentan exactamente el mismo costo mínimo de ruta desde el nodo del incidente, el sistema aplica un **orden determinista mediante su identificador técnico estable** para seleccionar el hospital.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-018`, `RN-HOS-005`.

#### `RN-HOS-007` — Confirmación del destino condicionada a la reserva confirmada de plaza
- **Enunciado:** El destino hospitalario de una emergencia solo queda confirmado cuando se confirma también la reserva de una plaza (`R`) en dicho hospital. **No se permite iniciar el traslado hacia el hospital sin un destino hospitalario confirmado.**
- **Estado:** **Aprobada**
- **Referencias:** `DEC-018`, `RN-HOS-004`, `RN-HOS-005`.

#### `RN-HOS-008` — Política de reintentos automáticos en reserva hospitalaria concurrente y finalización anticipada
- **Enunciado:** Una operación independiente de reserva hospitalaria permite **1 intento inicial** y **hasta 2 reintentos adicionales exclusivamente ante conflictos concurrentes**, lo que equivale a un **máximo de 3 intentos totales**, bajo las siguientes reglas:
  1. En cada reintento se deben reevaluar obligatoriamente: disponibilidad actual, capacidad disponible (`D`), elegibilidad hospitalaria, alcanzabilidad en el grafo, costo de ruta (Dijkstra) y desempate determinista.
  2. La operación finaliza inmediatamente cuando se confirma una reserva válida o cuando ya no quedan hospitales elegibles (sin consumir reintentos vacíos).
  3. Si desde el inicio no existen hospitales elegibles, la emergencia pasa a espera de destino sin consumir reintentos por concurrencia (`0` reintentos).
  4. Si se agotan el intento inicial y los 2 reintentos adicionales por conflictos concurrentes, la emergencia pasa a espera de destino registrando un motivo funcional distinguible (por ejemplo, `HOSPITAL_RESERVATION_RETRY_EXHAUSTED`).
  5. Los errores técnicos generales no deben clasificarse automáticamente como conflictos concurrentes.
  6. Esta política es **independiente de `DEC-017`**: no modifica los límites de reintentos para la asignación automática de ambulancias (`RN-CON-005`). *(La implementación de atomicidad, bloqueos, transacciones, restricciones SQL y mecanismos de retry técnico corresponde a `PEN-006`).*
- **Estado:** **Aprobada**
- **Referencias:** `DEC-017`, `DEC-018`, `PEN-006`, `RN-CON-005`, `RN-HOS-004` a `RN-HOS-007`.

#### `RN-HOS-009` — Espera de destino hospitalario y distinción respecto a la espera de ambulancia
- **Enunciado:** Cuando no exista hospital elegible o no se logre confirmar una reserva tras agotar los reintentos por conflicto concurrente:
  1. La emergencia permanece activa.
  2. Conserva la ambulancia asignada y ocupada.
  3. No inicia el traslado hacia el hospital.
  4. Entra en **espera de destino hospitalario**.
  5. Se registra el motivo funcional correspondiente.
  Esta condición no debe confundirse con la espera de asignación de ambulancia (`RN-ASI-004`). *(El nombre definitivo del estado se resolverá junto con la máquina de estados en `PEN-002d`).*
- **Estado:** **Aprobada**
- **Referencias:** `DEC-018`, `PEN-002d`, `RN-ASI-004`, `RN-HOS-007`, `RN-HOS-008`.

#### `RN-HOS-010` — Cola FIFO de espera de destino hospitalario
- **Enunciado:** Las emergencias que se encuentran esperando destino hospitalario se ordenan según el momento de ingreso a dicha espera (disciplina FIFO). Ante igualdad exacta en el momento de ingreso, se aplica un orden determinista mediante un identificador estable.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-018`, `RN-HOS-009`.

#### `RN-HOS-011` — Reevaluación automática de la cola hospitalaria sin pérdida de posición e independencia del contador
- **Enunciado:** Cuando se libera capacidad hospitalaria, ARE activa una reevaluación automática de las emergencias en espera de destino bajo las siguientes reglas:
  1. La reevaluación respeta el orden FIFO (`RN-HOS-010`).
  2. Si la primera emergencia de la cola no tiene ningún hospital elegible (por ejemplo, por inalcanzabilidad hacia el hospital que liberó plaza), dicha emergencia **permanece en espera conservando su posición original** y se permite evaluar la siguiente emergencia de la cola. Una reevaluación fallida no desplaza automáticamente una emergencia al final de la cola.
  3. Cada nueva operación independiente de reserva utiliza su propio contador de intentos hospitalarios (hasta 1 intento inicial + 2 reintentos adicionales), sin acumular los intentos de operaciones anteriores.
  *(La política técnica de ejecución concurrente de la cola queda pendiente en `PEN-006`).*
- **Estado:** **Aprobada**
- **Referencias:** `DEC-018`, `PEN-006`, `RN-HOS-008`, `RN-HOS-009`, `RN-HOS-010`.

#### `RN-HOS-012` — Transformación de reserva en ocupación a la llegada al hospital
- **Enunciado:** Cuando la ambulancia llega al hospital con una reserva confirmada:
  1. La reserva se transforma en ocupación.
  2. Se reduce `R` en una unidad (`R = R - 1`).
  3. Se incrementa `O` en una unidad (`O = O + 1`).
  4. La disponibilidad `D = C - R - O` permanece sin cambios.
  La llegada no consume una segunda plaza, y la reserva confirmada permanece válida aunque el hospital se llene posteriormente (`D = 0`) por otras reservas u ocupaciones.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-007`, `DEC-018`, `RN-HOS-004`, `RN-RUT-005`.

#### `RN-HOS-013` — Alta simulada y liberación de plaza ocupada
- **Enunciado:** La plaza ocupada (`O`) se libera mediante un **evento explícito de alta simulada** activado por un operador autorizado, cumpliendo las siguientes condiciones:
  1. Requiere una ocupación válida.
  2. Libera exactamente una plaza ocupada (`O = O - 1`).
  3. No puede ejecutarse dos veces con efecto sobre la misma ocupación.
  4. Incrementa la disponibilidad en una unidad (`D = D + 1`).
  5. Desencadena la reevaluación automática de las emergencias en espera de destino hospitalario (`RN-HOS-011`).
  6. No representa un alta médica real (`DEC-001`, `DEC-007`).
  *(Nota: los permisos exactos del actor que activa el alta simulada quedan pendientes de definición en la matriz de responsabilidades y permisos).*
- **Estado:** **Aprobada**
- **Referencias:** `DEC-001`, `DEC-007`, `DEC-018`, `RN-HOS-004`, `RN-HOS-011`, `docs/product/stakeholders.md` (Sección 5).

#### `RN-HOS-014` — Independencia entre liberación de plaza hospitalaria y liberación de ambulancia
- **Enunciado:** La liberación de una plaza hospitalaria mediante alta simulada **no implica la liberación de una ambulancia**. El momento exacto de liberación de la ambulancia y los estados definitivos de su ciclo de vida permanecen pendientes de definición en `PEN-002d`.
- **Estado:** **Aprobada**
- **Referencias:** `DEC-018`, `PEN-002d`, `RN-HOS-013`.

#### `RN-HOS-015` — Prohibición de modificar la capacidad hospitalaria durante escenarios experimentales activos
- **Enunciado:** Se prohíbe modificar la capacidad hospitalaria (`C`) durante la ejecución de escenarios experimentales activos. Fuera de escenarios experimentales activos, cualquier modificación de la capacidad hospitalaria debe preservar las invariantes de capacidad (`R + O <= C` y `D = C - R - O >= 0`, `RN-HOS-004`). *(Esta restricción no se extiende automáticamente a otros catálogos cuyo comportamiento durante escenarios activos continúa pendiente).*
- **Estado:** **Aprobada**
- **Referencias:** `DEC-016`, `DEC-018`, `RN-HOS-004`, `docs/product/stakeholders.md` (`ACT-03`, Sección 5.2).

#### Criterios de aceptación funcionales aprobados para hospitales (`CA-HOS-01` a `CA-HOS-13`)

- **`CA-HOS-01` (Hospital más cercano sin capacidad):** Si al confirmar la recogida de la emergencia el hospital con menor costo de ruta tiene `D = 0` (`R + O = C`), el sistema lo descarta como no elegible y selecciona el siguiente hospital habilitado, alcanzable y con `D >= 1` de menor costo de ruta por Dijkstra.
- **`CA-HOS-02` (Empate de costos):** Si dos o más hospitales elegibles tienen exactamente el mismo costo mínimo de ruta por Dijkstra desde el nodo del incidente, el sistema selecciona de manera determinista aquel que preceda según su identificador técnico estable.
- **`CA-HOS-03` (Hospital inaccesible):** Si un hospital está habilitado y tiene plazas disponibles (`D >= 1`), pero no existe camino alcanzable en el grafo simulado desde el nodo del incidente, dicho hospital no se considera elegible ni puede ser seleccionado.
- **`CA-HOS-04` (Competencia por última plaza):** Si dos operaciones concurrentes intentan reservar la última plaza disponible (`D = 1`) de un mismo hospital, a lo sumo una confirma la reserva (`R` aumenta en 1 y `D` pasa a `0`), impidiendo sobreocupación (`R + O <= C`) y doble reserva de la misma plaza, mientras la otra operación reevalúa alternativas dentro de su límite de hasta 2 reintentos adicionales.
- **`CA-HOS-05` (Agotamiento de reintentos):** Si en una operación independiente de reserva hospitalaria fallan el intento inicial y los 2 reintentos adicionales (3 intentos totales) por conflictos concurrentes, la emergencia no inicia traslado, entra en espera de destino hospitalario conservando su ambulancia asignada y se registra un motivo funcional distinguible (como `HOSPITAL_RESERVATION_RETRY_EXHAUSTED`).
- **`CA-HOS-06` (Finalización anticipada):** El ciclo de reintentos hospitalarios se detiene inmediatamente en cuanto se confirma una reserva válida o cuando tras una reevaluación ya no quedan hospitales elegibles, sin consumir reintentos vacíos restantes.
- **`CA-HOS-07` (Ausencia de hospitales elegibles):** Si al confirmar la recogida no existe ningún hospital elegible desde el intento inicial, la emergencia entra directamente en espera de destino hospitalario conservando la ambulancia asignada, sin iniciar traslado y consumiendo `0` reintentos por concurrencia.
- **`CA-HOS-08` (Recuperación automática FIFO):** Cuando se libera capacidad hospitalaria (por ejemplo, tras un alta simulada), el sistema reevalúa automáticamente la cola de espera de destino hospitalario en estricto orden FIFO (y por identificador estable en caso de empate temporal), iniciando para cada operación independiente un contador nuevo de intentos hospitalarios desde cero.
- **`CA-HOS-09` (Primera emergencia sin hospital elegible):** Si durante la reevaluación automática de la cola FIFO la primera emergencia en espera no cuenta con ningún hospital elegible (por ejemplo, por inalcanzabilidad hacia el hospital con plaza liberada), dicha emergencia conserva su posición original en la cola sin ser enviada al final y el sistema evalúa la siguiente emergencia en espera.
- **`CA-HOS-10` (Transformación de reserva en ocupación):** Cuando la ambulancia arriba al hospital con reserva confirmada, la reserva se transforma en ocupación (`R` disminuye en 1, `O` aumenta en 1 y `D` permanece igual), sin consumir una segunda plaza y manteniendo su validez aunque el hospital haya quedado con `D = 0` antes de la llegada.
- **`CA-HOS-11` (Alta simulada válida y duplicada):** Al ejecutar un alta simulada sobre una ocupación válida, `O` disminuye en 1, `D` aumenta en 1 y se dispara la reevaluación automática de la cola de espera de destino; si se intenta ejecutar nuevamente el alta simulada sobre la misma ocupación ya liberada, el sistema rechaza o ignora el duplicado sin volver a alterar `O` ni `D`.
- **`CA-HOS-12` (Bloqueo de cambios de capacidad durante escenario activo):** Cualquier intento de modificar la capacidad total `C` de un hospital mientras un escenario experimental está activo es rechazado; fuera de escenarios activos, solo se permite modificar `C` si se preserva la invariante `R + O <= C`.
- **`CA-HOS-13` (Independencia entre plaza hospitalaria y ambulancia):** Ejecutar un alta simulada libera exclusivamente la plaza hospitalaria ocupada (`O`) sin liberar ni cambiar implícitamente el estado de una ambulancia, quedando el momento exacto de liberación de la ambulancia regido por lo que se defina en `PEN-002d`.

> **Asuntos hospitalarios y relacionados expresamente pendientes de definición (`PEN-002d`, `PEN-005`, `PEN-006` y `stakeholders.md` Secc. 5):** Se mantienen pendientes sin asumir comportamientos no aprobados: (1) cancelación de reservas confirmadas, (2) reasignación de destinos ya reservados, (3) permisos exactos del actor autorizado para activar altas simuladas, (4) nombres definitivos de los estados del ciclo de emergencia y ambulancia, (5) momento exacto de liberación de ambulancias, (6) mecanismos técnicos de concurrencia y persistencia y (7) configuración temporal del motor de simulación.

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

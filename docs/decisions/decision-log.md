# Bitácora de Decisiones — ARE (Arequipa RescUE)

**Estado del documento:** Aprobado — Versión inicial aceptada de la Primera Entrega Documental (`DEC-015`)
**Propósito:** Registrar de manera trazable las decisiones arquitectónicas, tecnológicas, funcionales y de producto formalmente acordadas en **ARE – Arequipa RescUE**, diferenciándolas estrictamente de las propuestas y definiciones que permanecen pendientes de aprobación.

> [!IMPORTANT]
> Solo llevan el estado **Aprobada** aquellas decisiones que cuentan con autorización expresa del responsable humano del proyecto. Las decisiones previas (`DEC-001` a `DEC-006`) se conservan íntegras y se amplían con los acuerdos funcionales y de gobernanza documental aprobados (`DEC-007` a `DEC-015`). Ningún parámetro o regla pendiente (`PEN-002a` a `PEN-008`, `RN-ASI-005`, `RN-MET-003`) se registra como decisión aprobada.

---

## 1. Resumen de Decisiones Acordadas

| ID | Título | Categoría | Estado | Fecha |
| :--- | :--- | :--- | :--- | :--- |
| [DEC-001](#dec-001-are-será-exclusivamente-un-simulador-académico) | ARE será exclusivamente un simulador académico | Producto / Alcance | Aprobada | Pendiente de precisar (registrada en sesión: 2026-11-12) |
| [DEC-002](#dec-002-arquitectura-de-monolito-modular-con-principios-hexagonales-y-mvc-en-presentación) | Se utilizará una arquitectura de monolito modular con principios hexagonales y MVC en presentación | Arquitectura | Aprobada | Pendiente de precisar (registrada en sesión: 2026-11-12) |
| [DEC-003](#dec-003-separación-estricta-entre-dominio-y-persistencia-jpa) | Se mantendrá una separación estricta entre dominio y persistencia JPA | Arquitectura / Persistencia | Aprobada | Pendiente de precisar (registrada en sesión: 2026-11-12) |
| [DEC-004](#dec-004-idioma-de-documentación-adr-interfaz-e-identificadores-técnicos) | La documentación y los ADR estarán en español y los identificadores técnicos en inglés | Convenciones / Documentación | Aprobada | Pendiente de precisar (registrada en sesión: 2026-11-12) |
| [DEC-005](#dec-005-stack-tecnológico-base-del-proyecto) | Adopción del stack tecnológico base (Java, Spring Boot, Spring MVC, Thymeleaf, Spring Data JPA, Hibernate, MySQL y Maven) | Tecnología | Aprobada (versiones pendientes) | Pendiente de precisar (registrada en sesión: 2026-11-12) |
| [DEC-006](#dec-006-contrato-de-trabajo-agentsmd-y-gobernanza-con-odd-engram-y-judgment-day) | Aprobación de `AGENTS.md` y gobernanza mediante ODD, Engram y Judgment Day | Metodología / Gobernanza | Aprobada | 2026-11-12 |
| [DEC-007](#dec-007-propósito-integral-nivel-de-realismo-intermedio-y-límites-del-ciclo-simulado) | Propósito integral, nivel de realismo intermedio y límites del ciclo simulado (registro hasta llegada al hospital) | Producto / Alcance | Aprobada | 2026-11-12 |
| [DEC-008](#dec-008-alcance-aprobado-del-mvp-y-exclusiones-explícitas) | Alcance aprobado del MVP y exclusiones explícitas | Producto / MVP | Aprobada | 2026-11-12 |
| [DEC-009](#dec-009-definición-de-tres-actores-humanos-y-motor-de-simulación-como-componente-interno) | Definición de tres actores humanos y motor de simulación como componente interno | Producto / Actores | Aprobada | 2026-11-12 |
| [DEC-010](#dec-010-triage-simulado-mediante-reglas-configurables-reproducibles-y-trazables) | Triage simulado mediante reglas configurables, reproducibles y trazables | Dominio / Triage | Aprobada (parámetros pendientes) | 2026-11-12 |
| [DEC-011](#dec-011-modalidades-de-asignación-preservación-de-ambulancias-especializadas-y-reintentos-ante-concurrencia) | Modalidades de asignación, preservación de ambulancias especializadas y reintentos ante concurrencia | Dominio / Despacho y Concurrencia | Aprobada (límite de reintentos pendiente) | 2026-11-12 |
| [DEC-012](#dec-012-gestión-de-cola-de-emergencias-pendientes-priorización-por-equipamiento-y-fifo-sin-aging-en-v1) | Gestión de cola de emergencias pendientes, priorización por equipamiento y FIFO sin aging en v1 | Dominio / Cola y Métricas | Aprobada | 2026-11-12 |
| [DEC-013](#dec-013-cálculo-de-rutas-mediante-dijkstra-sobre-red-vial-simplificada-de-arequipa) | Cálculo de rutas mediante Dijkstra sobre red vial simplificada de Arequipa | Dominio / Rutas | Aprobada | 2026-11-12 |
| [DEC-014](#dec-014-gestión-hospitalaria-básica-y-desacoplamiento-de-la-selección-inicial-de-ambulancias) | Gestión hospitalaria básica y desacoplamiento respecto a la selección inicial de ambulancias | Dominio / Hospitales | Aprobada (reglas de hospital pendientes) | 2026-11-12 |
| [DEC-015](#dec-015-aprobación-formal-de-la-primera-entrega-documental-definición-de-producto-y-reglas-de-negocio) | Aprobación formal de la primera entrega documental (definición de producto y reglas de negocio) | Producto / Gobernanza Documental | Aprobada (con propuestas y parámetros pendientes abiertos) | 2026-11-12 |

---

## 2. Detalle de Decisiones Aprobadas

### DEC-001: ARE será exclusivamente un simulador académico

- **Identificador:** DEC-001
- **Fecha:** Pendiente de precisar fecha oficial en actas académicas (registrada en el entorno el 2026-11-12).
- **Título:** ARE será exclusivamente un simulador académico.
- **Contexto:** El proyecto **ARE – Arequipa RescUE** (`ProyectoFinalTO-Arequipa-RescUE`) se desarrolla en un marco académico de ingeniería de software para estudiar el proceso de atención de emergencias médicas, la gestión de ambulancias, la administración de recursos hospitalarios y el control de concurrencia. Resulta indispensable delimitar la naturaleza del sistema para evitar que se interprete o evalúe como un sistema clínico o de despacho real.
- **Decisión:** Establecer que ARE es única y exclusivamente un simulador académico de atención de emergencias médicas y gestión de ambulancias. No se utilizará para despachar unidades reales ni para apoyar o ejecutar decisiones clínicas reales.
- **Justificación:** Permite enfocar el esfuerzo de ingeniería en el diseño arquitectónico, la calidad estructural, la consistencia de datos ante eventos concurrentes y la verificabilidad mediante pruebas, sin asumir responsabilidades médico-legales, normativas sanitarias ni integraciones de hardware de misión crítica fuera del propósito formativo.
- **Consecuencias:**
  - **Positivas:** Claridad en el alcance del proyecto; libertad controlada para modelar escenarios de simulación concurrente y evaluar algoritmos; exclusión explícita de integraciones reales de telecomunicaciones o telemetría médica.
  - **Restricciones/Compromisos:** Toda la documentación, interfaz de usuario y artefactos del repositorio deben reflejar su carácter de simulador académico; los datos tratados deben ser simulados o sintéticos.
- **Estado:** Aprobada.
- **Referencias relacionadas:** `AGENTS.md` (Secciones 1.1, 1.2, 1.3), `docs/product/vision-and-scope.md` (Secciones 1, 3, 9), `docs/product/requirements/business-rules.md` (`RN-EME-005`).

---

### DEC-002: Arquitectura de monolito modular con principios hexagonales y MVC en presentación

- **Identificador:** DEC-002
- **Fecha:** Pendiente de precisar fecha oficial en actas académicas (registrada en el entorno el 2026-11-12).
- **Título:** Se utilizará una arquitectura de monolito modular con principios hexagonales y MVC en presentación.
- **Contexto:** El simulador requiere organizar múltiples ámbitos funcionales (emergencias, ambulancias, despacho, hospitales, simulación, rutas) manteniendo alta cohesión, bajo acoplamiento, facilidad de pruebas del núcleo de negocio y simplicidad de despliegue académico.
- **Decisión:** Adoptar una arquitectura de **monolito modular** aplicando principios de **arquitectura hexagonal (Ports and Adapters)** para aislar el dominio y la aplicación de los detalles técnicos, utilizando el patrón **MVC (Spring MVC + Thymeleaf)** exclusivamente en la capa de presentación. Se prohíbe expresamente introducir microservicios.
- **Justificación:** El monolito modular evita la complejidad accidental de sistemas distribuidos (latencia de red, transacciones distribuidas, despliegue múltiple) y permite estudiar transacciones locales y concurrencia sobre una base de datos relacional compartida, mientras que la arquitectura hexagonal protege las reglas de negocio y facilita pruebas unitarias rápidas sin levantar la infraestructura.
- **Consecuencias:**
  - **Positivas:** Despliegue unificado; fronteras claras de responsabilidad; dependencias dirigidas hacia el interior (`presentation`/`infrastructure` → `application` → `domain`); facilidad para probar el dominio de forma aislada.
  - **Restricciones/Compromisos:** Exige disciplina para impedir que los módulos accedan directamente a la infraestructura interna de otros módulos; los límites definitivos de cada módulo y los subpaquetes de puertos deberán validarse y documentarse mediante ADR antes de la implementación.
- **Estado:** Aprobada.
- **Referencias relacionadas:** `AGENTS.md` (Secciones 2.1, 2.4, 3.1, 3.2, 3.3), `docs/product/vision-and-scope.md` (Sección 10).

---

### DEC-003: Separación estricta entre dominio y persistencia JPA

- **Identificador:** DEC-003
- **Fecha:** Pendiente de precisar fecha oficial en actas académicas (registrada en el entorno el 2026-11-12).
- **Título:** Se mantendrá una separación estricta entre dominio y persistencia JPA.
- **Contexto:** En aplicaciones empresariales con Spring Data JPA y Hibernate es frecuente acoplar el modelo de negocio a las anotaciones de persistencia (`@Entity`, `@Table`, carga perezosa, constructores vacíos obligatorios) y exponer dichas entidades hasta los controladores o vistas web, degradando la encapsulación de las invariantes del negocio.
- **Decisión:** Mantener una separación estricta entre:
  1. **Modelo de dominio (`domain/`):** Clases puras de Java que representan conceptos e invariantes del negocio, libres de dependencias de Spring, JPA, Hibernate o Servlet.
  2. **Modelo de persistencia JPA (`infrastructure/`):** Entidades anotadas con JPA/Hibernate dedicadas a la representación relacional.
  3. **Adaptadores de persistencia (`infrastructure/`):** Componentes que implementan los puertos de persistencia y realizan la traducción explícita entre el modelo de dominio y las entidades JPA.
  Asimismo, las entidades JPA no deben exponerse directamente a los controladores MVC ni a las vistas Thymeleaf.
- **Justificación:** Garantiza que el dominio pueda diseñarse y probarse unitariamente sin iniciar el contexto de Spring ni conectarse a MySQL, preserva las invariantes del negocio frente a restricciones del ORM y desacopla los contratos de presentación (DTO/formularios) del esquema de base de datos.
- **Consecuencias:**
  - **Positivas:** Independencia total del núcleo de negocio; pruebas unitarias de dominio rápidas y deterministas; evolución controlada del esquema relacional sin romper la capa de presentación.
  - **Restricciones/Compromisos:** Requiere escribir y mantener mapeadores explícitos entre entidades de dominio y entidades JPA, así como DTOs en la capa de presentación.
- **Estado:** Aprobada.
- **Referencias relacionadas:** `AGENTS.md` (Secciones 2.3, 4.1, 4.2, 4.3, 5.2), `docs/product/vision-and-scope.md` (Sección 10).

---

### DEC-004: Idioma de documentación, ADR, interfaz e identificadores técnicos

- **Identificador:** DEC-004
- **Fecha:** Pendiente de precisar fecha oficial en actas académicas (registrada en el entorno el 2026-11-12).
- **Título:** La documentación y los ADR estarán en español y los identificadores técnicos en inglés.
- **Contexto:** El proyecto se evalúa en un entorno académico hispanohablante, mientras que el ecosistema de Java, Spring Boot y las convenciones estándar de código fuente utilizan el inglés para los identificadores técnicos.
- **Decisión:**
  - Redactar toda la documentación del proyecto, la bitácora de decisiones y los ADR en **español neutro/profesional**.
  - Redactar los textos visibles de la interfaz de usuario (plantillas Thymeleaf, etiquetas, formularios y mensajes orientados al usuario) en **español neutro/profesional**.
  - Escribir todos los identificadores técnicos del código fuente (paquetes, clases, interfaces, métodos, variables y constantes) en **inglés**, siguiendo las convenciones estándar de Java.
  - Utilizar español neutro/profesional o inglés de forma consistente en comentarios de código y Javadoc, sin jerga ni modismos regionales.
- **Justificación:** Facilita la revisión académica y funcional de los artefactos documentales y de la interfaz de usuario en el idioma del curso, a la vez que evita mezclar idiomas en la sintaxis del código (`getAmbulancia()`, etc.) y mantiene compatibilidad con las buenas prácticas de Java.
- **Consecuencias:**
  - **Positivas:** Coherencia lingüística tanto en los entregables documentales como en la base de código; alineación explícita con las reglas del entorno de agentes.
  - **Restricciones/Compromisos:** Requiere mantener un glosario de términos de dominio que vincule los conceptos funcionales en español con sus identificadores equivalentes en inglés dentro del código.
- **Estado:** Aprobada.
- **Referencias relacionadas:** `AGENTS.md` (Sección 8.1), `docs/product/vision-and-scope.md` (Sección 10).

---

### DEC-005: Stack tecnológico base del proyecto

- **Identificador:** DEC-005
- **Fecha:** Pendiente de precisar fecha oficial en actas académicas (registrada en el entorno el 2026-11-12).
- **Título:** Adopción del stack tecnológico base (Java, Spring Boot, Spring MVC, Thymeleaf, Spring Data JPA, Hibernate, MySQL y Maven).
- **Contexto:** Se requiere definir las tecnologías centrales sobre las cuales se construirá el simulador ARE, asegurando soporte para aplicaciones web MVC, persistencia relacional transaccional y gestión estandarizada de construcción.
- **Decisión:** Fijar como stack tecnológico obligatorio:
  - **Lenguaje:** Java.
  - **Framework principal:** Spring Boot.
  - **Presentación web:** Spring MVC y motor de plantillas Thymeleaf.
  - **Persistencia:** Spring Data JPA y Hibernate.
  - **Base de datos relacional:** MySQL.
  - **Construcción y dependencias:** Maven.
  Cualquier cambio en este stack o la incorporación de librerías adicionales requiere justificación técnica y autorización expresa del responsable del proyecto. Las versiones concretas de cada tecnología quedan pendientes de verificación y aprobación antes de inicializar el proyecto.
- **Justificación:** Conjunto maduro, ampliamente soportado en entornos académicos y empresariales, que provee las capacidades necesarias para implementar el monolito modular, gestionar transacciones sobre MySQL y renderizar vistas del lado del servidor sin complejidades de arquitecturas frontend separadas no solicitadas.
- **Consecuencias:**
  - **Positivas:** Ecosistema cohesivo e integrado; soporte nativo para transacciones, bloqueo optimista/pesimista y pruebas por capas.
  - **Restricciones/Compromisos:** No se debe inicializar el proyecto ni fijar el archivo `pom.xml` hasta que se aprueben explícitamente las versiones de Java, Spring Boot y MySQL.
- **Estado:** Aprobada (con versiones específicas pendientes de aprobación).
- **Referencias relacionadas:** `AGENTS.md` (Secciones 2.2, 2.4), `docs/product/vision-and-scope.md` (Sección 10).

---

### DEC-006: Contrato de trabajo `AGENTS.md` y gobernanza con ODD, Engram y Judgment Day

- **Identificador:** DEC-006
- **Fecha:** 2026-11-12 (aprobación formal registrada en sesión y en Engram con `sync_id: obs-0992398d05ed8b31`).
- **Título:** Aprobación de `AGENTS.md` como contrato de trabajo e integración de ODD, Engram y Judgment Day.
- **Contexto:** Para mantener el control humano sobre las decisiones de ingeniería y evitar que asistentes de IA asuman requisitos no aprobados o introduzcan cambios fuera de alcance, se requiere un contrato operativo explícito en la raíz del repositorio.
- **Decisión:** Aprobar formalmente `AGENTS.md` como contrato de trabajo del proyecto ARE, estableciendo el flujo Organic-Driven Development (ODD: explorar, autorizar, clasificar, planificar, implementar incrementalmente y verificar con línea `Risk:`), el uso de Engram para persistir el contexto y decisiones aprobadas, y el uso de Judgment Day (`jd-judge-a`, `jd-judge-b` y `jd-fix-agent` con límite de dos rondas) para auditorías independientes de alto riesgo.
- **Justificación:** Asegura trazabilidad, disciplina de ingeniería, preservación del contexto entre sesiones y validación crítica independiente antes de consolidar decisiones arquitectónicas o de concurrencia.
- **Consecuencias:**
  - **Positivas:** Prevención de desviaciones de alcance; distinción obligatoria entre lo aprobado y lo propuesto; auditoría estructurada en decisiones críticas.
  - **Restricciones/Compromisos:** La aprobación de `AGENTS.md` no autoriza automáticamente el inicio de la implementación del simulador ni aprueba las decisiones técnicas listadas como pendientes.
- **Estado:** Aprobada.
- **Referencias relacionadas:** `AGENTS.md` (Secciones 9, 10, 11, 12).

---

### DEC-007: Propósito integral, nivel de realismo intermedio y límites del ciclo simulado

- **Identificador:** DEC-007
- **Fecha:** 2026-11-12.
- **Título:** Simulación integral del ciclo de emergencias médicas con nivel de realismo intermedio, geografía simplificada inspirada en Arequipa y cierre del ciclo al llegar al hospital.
- **Contexto:** En la versión inicial de descubrimiento era necesario precisar hasta dónde llegaría la representación del proceso de emergencia médica y con qué grado de abstracción geográfica y operativa se modelaría Arequipa.
- **Decisión:**
  - El objetivo del simulador es representar integralmente el ciclo de emergencias médicas poniendo énfasis en la concurrencia y la asignación de recursos.
  - Se adoptará un **nivel de realismo intermedio** sobre una **representación geográfica simplificada inspirada en Arequipa**.
  - El ciclo simulado inicia con el **registro de la emergencia** y concluye con la **llegada al hospital**.
  - No se simularán procesos posteriores ni clínicos como hospitalización interna, tratamiento médico ni atención clínica real.
- **Justificación:** Acota el dominio al problema central de despacho, ruteo y concurrencia de recursos móviles/hospitalarios sin dispersar el esfuerzo en procesos clínicos intrahospitalarios ajenos al objetivo académico.
- **Consecuencias:**
  - **Positivas:** Frontera de inicio y fin del proceso claramente verificable; modelo geográfico manejable y autocontenido.
  - **Restricciones/Compromisos:** Cualquier flujo posterior a la llegada de la unidad al hospital (evolución clínica, alta médica, cirugía) queda excluido del sistema.
- **Estado:** Aprobada.
- **Referencias relacionadas:** `docs/product/vision-and-scope.md` (Secciones 3, 7, 8, 9), `docs/product/requirements/business-rules.md` (`RN-EME-001`, `RN-RUT-005`).

---

### DEC-008: Alcance aprobado del MVP y exclusiones explícitas

- **Identificador:** DEC-008
- **Fecha:** 2026-11-12.
- **Título:** Definición del alcance funcional del MVP equilibrado y elementos excluidos del MVP.
- **Contexto:** Estaba pendiente (`PEN-001`) determinar qué capacidades de las seis áreas preliminares entrarían en el Producto Mínimo Viable (MVP) y qué elementos debían quedar expresamente fuera.
- **Decisión:**
  - **Incluir en el MVP:**
    1. Gestión de emergencias.
    2. Gestión de ambulancias.
    3. Gestión básica de hospitales.
    4. Asignación supervisada y automática.
    5. Rutas mediante el algoritmo de Dijkstra.
    6. Seguimiento simulado.
    7. Simulación concurrente.
    8. Métricas experimentales.
  - **Mantener explícitamente fuera del MVP:** GPS y telemetría reales, tráfico real, telefonía real, datos reales de pacientes, decisiones clínicas reales, microservicios y el algoritmo A* como requisito obligatorio.
- **Justificación:** Un MVP equilibrado que integre de forma acotada las capacidades esenciales permite demostrar el flujo completo y evaluar empíricamente la concurrencia y el despacho sin depender de integraciones externas ni sobre-ingeniería.
- **Consecuencias:**
  - **Positivas:** Resuelve `PEN-001` y alinea todos los artefactos de requisitos sobre un mismo alcance funcional confirmado.
  - **Restricciones/Compromisos:** La profundidad de cada módulo debe mantenerse en nivel de realismo intermedio para garantizar completitud y calidad de pruebas.
- **Estado:** Aprobada.
- **Referencias relacionadas:** `docs/product/vision-and-scope.md` (Secciones 8 y 9), `docs/product/stakeholders.md`.

---

### DEC-009: Definición de tres actores humanos y motor de simulación como componente interno

- **Identificador:** DEC-009
- **Fecha:** 2026-11-12.
- **Título:** Establecimiento de tres actores humanos (Operador de recepción, Despachador y Administrador) y del motor de simulación como componente interno.
- **Contexto:** En el análisis preliminar de `vision-and-scope.md` se habían listado cinco actores tentativos (incluyendo un rol separado de evaluador/docente y tratando al motor de simulación en la misma tabla de actores). Se requería definir los actores definitivos y sus fronteras de responsabilidad.
- **Decisión:**
  - Documentar exactamente **tres actores humanos**:
    1. **Operador de recepción:** registra emergencias y consulta información y estados.
    2. **Despachador:** revisa emergencias, gestiona asignaciones y supervisa despachos.
    3. **Administrador:** gestiona ambulancias, hospitales, reglas de clasificación y parámetros del simulador; configura y ejecuta escenarios experimentales y consulta métricas.
  - Establecer que el **motor de simulación es un componente interno del sistema**, no un actor humano.
- **Justificación:** Separa limpiamente la recepción de incidentes, la toma de decisiones operativas de despacho y la administración/experimentación académica, evitando duplicar actores de observación y distinguiendo los sujetos humanos de los subsistemas internos.
- **Consecuencias:**
  - **Positivas:** Reemplaza la lista preliminar de cinco actores en `vision-and-scope.md` y estructura de forma directa los futuros requisitos funcionales, casos de uso e historias de usuario.
  - **Restricciones/Compromisos:** El Despachador no administra catálogos ni lanza experimentos; el Administrador concentra la configuración de escenarios experimentales y la consulta de métricas.
- **Estado:** Aprobada.
- **Referencias relacionadas:** `docs/product/stakeholders.md`, `docs/product/vision-and-scope.md` (Sección 6).

---

### DEC-010: Triage simulado mediante reglas configurables, reproducibles y trazables

- **Identificador:** DEC-010
- **Fecha:** 2026-11-12.
- **Título:** Clasificación de emergencias (triage simulado) basada en reglas configurables, deterministas/reproducibles y auditables.
- **Contexto:** El simulador necesita clasificar las solicitudes de emergencia y determinar el equipamiento requerido para el despacho sin realizar diagnósticos médicos reales ni usar lógica opaca no verificable.
- **Decisión:**
  - Utilizar **reglas configurables** (administradas por el Administrador) a partir de las cuales el sistema determine una clasificación simulada y los requisitos de equipamiento según los datos registrados de la emergencia.
  - Garantizar **reproducibilidad**: los mismos datos de entrada evaluados con la misma versión de reglas deben producir exactamente el mismo resultado.
  - Garantizar **trazabilidad**: debe poder identificarse qué reglas o versión de configuración se aplicaron a cada emergencia clasificada.
  - La clasificación no representa diagnóstico ni decisión clínica real.
  - Los niveles, variables de entrada, categorías y reglas concretas quedan pendientes de definición.
- **Justificación:** Permite experimentar con distintas configuraciones de clasificación en escenarios académicos y auditar por qué una emergencia exigió determinado equipamiento en pruebas de concurrencia.
- **Consecuencias:**
  - **Positivas:** Determinismo en pruebas automatizadas y experimentos; desacoplamiento entre el mecanismo de evaluación de triage y el catálogo concreto de reglas.
  - **Restricciones/Compromisos:** Requiere conservar la referencia a la versión/configuración de reglas aplicada al clasificar cada solicitud.
- **Estado:** Aprobada (con niveles, variables, categorías y reglas concretas pendientes de definición).
- **Referencias relacionadas:** `docs/product/requirements/business-rules.md` (`RN-EME-002` a `RN-EME-007`).

---

### DEC-011: Modalidades de asignación, preservación de ambulancias especializadas y reintentos ante concurrencia

- **Identificador:** DEC-011
- **Fecha:** 2026-11-12.
- **Título:** Reglas de elegibilidad, recomendación por costo de desplazamiento, preservación de unidades especializadas, despacho supervisado/automático y manejo de conflictos concurrentes.
- **Contexto:** El núcleo del estudio académico de ARE reside en la asignación de ambulancias bajo solicitudes simultáneas, donde deben coordinarse criterios de compatibilidad técnica, eficiencia de ruta, resguardo de recursos escasos y resolución de colisiones concurrentes.
- **Decisión:**
  1. **Dos modalidades de asignación:** supervisada y automática.
  2. **Elegibilidad previa a recomendación:** la elegibilidad se determina antes de recomendar unidades y considera disponibilidad, estado operativo y equipamiento requerido.
  3. **Comparación por costo:** entre las ambulancias elegibles, el sistema calcula y compara el costo de desplazamiento.
  4. **Preservación de unidades especializadas:** se prefieren ambulancias básicas cuando sean suficientes para la emergencia; una ambulancia especializada puede utilizarse para una emergencia básica únicamente si no existen alternativas básicas elegibles y la especializada cumple los requisitos.
  5. **Flexibilidad en modalidad supervisada:** el despachador puede seleccionar cualquier ambulancia elegible, aunque no sea la primera recomendada por el sistema.
  6. **Ejecución en modalidad automática:** el sistema selecciona y asigna la ambulancia sin confirmación humana.
  7. **Ausencia de unidades elegibles:** si no hay ambulancias elegibles, la emergencia permanece en estado pendiente.
  8. **Resolución de conflictos concurrentes:**
     - En general/modalidad automática: si una asignación concurrente falla porque la ambulancia dejó de estar disponible, el sistema reevalúa las alternativas y reintenta automáticamente con otra ambulancia elegible. Los reintentos serán limitados y, al agotarse, la emergencia permanece pendiente (el número máximo de reintentos queda pendiente de definición).
     - En modalidad supervisada: si la ambulancia seleccionada por el despachador deja de estar disponible antes de concretarse la asignación, el sistema actualiza las alternativas elegibles y solicita una nueva selección al despachador.
- **Justificación:** Combina realismo operativo (decisión humana asistida vs. despacho automatizado), uso racional de recursos escasos y un protocolo claro de recuperación frente a condiciones de carrera cuando dos procesos compiten por la misma ambulancia.
- **Consecuencias:**
  - **Positivas:** Comportamiento predecible y verificable ante colisiones de concurrencia; separación clara entre elegibilidad dura y orden de recomendación.
  - **Restricciones/Compromisos:** La capa de aplicación/dominio debe detectar de forma transaccional el cambio de disponibilidad de una unidad y disparar la actualización de alternativas (modo supervisado) o el ciclo acotado de reintento (modo automático).
- **Estado:** Aprobada (con el número máximo de reintentos pendiente de definición).
- **Referencias relacionadas:** `docs/product/requirements/business-rules.md` (`RN-ELE-001` a `RN-CON-005`).

---

### DEC-012: Gestión de cola de emergencias pendientes, priorización por equipamiento y FIFO sin aging en v1

- **Identificador:** DEC-012
- **Fecha:** 2026-11-12.
- **Título:** Orden de atención de la cola de emergencias al liberarse unidades especializadas, criterio FIFO y observación experimental de *starvation*.
- **Contexto:** Cuando existen emergencias en espera y una ambulancia especializada vuelve a estar disponible, el sistema necesita una política determinista para decidir qué emergencia pendiente atender sin confundir compatibilidad técnica con juicio médico.
- **Decisión:**
  - Cuando se libera una ambulancia especializada, **primero se consideran las emergencias pendientes que requieren específicamente ese equipamiento**.
  - Si no existen emergencias pendientes de ese grupo, **se consideran las emergencias básicas compatibles**.
  - Dentro de cada grupo se aplica estricto orden **FIFO según el momento de registro de la emergencia**.
  - La necesidad de equipamiento representa una restricción técnica de recursos del simulador y **no debe interpretarse como una prioridad clínica**.
  - No se aplicará envejecimiento (*aging*) automático en la primera versión del sistema.
  - Se registrarán tiempos de espera y métricas experimentales para observar y analizar posibles situaciones de inanición (*starvation*).
- **Justificación:** Permite estudiar académicamente el fenómeno de *starvation* y el impacto de las políticas de planificación de colas sobre recursos heterogéneos sin ocultarlo mediante heurísticas de *aging* en la primera versión.
- **Consecuencias:**
  - **Positivas:** Regla de despacho de cola 100 % determinista y auditable; generación de datos experimentales valiosos para el análisis académico del Administrador.
  - **Restricciones/Compromisos:** El sistema debe registrar con precisión las marcas de tiempo de registro y asignación para calcular los tiempos de espera por grupo.
- **Estado:** Aprobada.
- **Referencias relacionadas:** `docs/product/requirements/business-rules.md` (`RN-COL-001` a `RN-COL-006`, `RN-MET-002`).

---

### DEC-013: Cálculo de rutas mediante Dijkstra sobre red vial simplificada de Arequipa

- **Identificador:** DEC-013
- **Fecha:** 2026-11-12.
- **Título:** Adopción de Dijkstra como algoritmo inicial de rutas sobre grafo vial simplificado y distinción frente a mecanismos de concurrencia.
- **Contexto:** Estaba pendiente (`PEN-002`) definir cómo se representarían las ubicaciones y cómo se calcularía el costo de desplazamiento entre ambulancias, incidentes y hospitales.
- **Decisión:**
  - Utilizar el algoritmo de **Dijkstra** como algoritmo inicial obligatorio del MVP para calcular rutas y costos de desplazamiento sobre una representación simplificada en grafo de la red vial de Arequipa.
  - Mantener el algoritmo **A\*** únicamente como posible extensión futura o comparación experimental, no como requisito del MVP.
  - Establecer explícitamente que los algoritmos de cálculo de rutas no deben confundirse con los mecanismos de asignación concurrente y control transaccional.
- **Justificación:** Dijkstra garantiza encontrar el camino de costo mínimo sobre grafos con pesos no negativos de manera determinista, suficiente para evaluar elegibilidad/costo en el MVP, dejando la puerta abierta al patrón *Strategy* si en el futuro se compara con A*.
- **Consecuencias:**
  - **Positivas:** Modelo espacial autocontenido, reproducible y sin dependencia de APIs cartográficas externas; separación clara entre el cálculo algorítmico del camino y el control de concurrencia de la asignación.
  - **Restricciones/Compromisos:** Queda pendiente definir la topología concreta (nodos, aristas y pesos) del grafo simplificado de Arequipa.
- **Estado:** Aprobada.
- **Referencias relacionadas:** `docs/product/requirements/business-rules.md` (`RN-RUT-001` a `RN-RUT-006`).

---

### DEC-014: Gestión hospitalaria básica y desacoplamiento de la selección inicial de ambulancias

- **Identificador:** DEC-014
- **Fecha:** 2026-11-12.
- **Título:** Gestión básica de hospitales con disponibilidad simulada, sin intervenir en la selección inicial de ambulancias.
- **Contexto:** Era necesario precisar cómo interactúa la gestión de hospitales con el proceso de selección y despacho de ambulancias dentro del ciclo simulado.
- **Decisión:**
  - Implementar en el MVP una **gestión básica de hospitales**, incluyendo su ubicación en la red simplificada y su disponibilidad simulada.
  - Establecer que **la disponibilidad hospitalaria no interviene en la selección inicial de ambulancias**.
  - Dejar las reglas específicas de selección de hospital de destino y de gestión detallada de su capacidad como asuntos pendientes de definición.
- **Justificación:** Desacopla la urgencia de asignar y movilizar una ambulancia hacia el incidente respecto de la asignación del centro receptor, simplificando la transacción inicial de despacho de unidades.
- **Consecuencias:**
  - **Positivas:** Reduce el acoplamiento transaccional en el momento de seleccionar la ambulancia; corrige cualquier supuesto preliminar que vinculara la elegibilidad de la ambulancia con la reserva simultánea de hospital en el primer paso.
  - **Restricciones/Compromisos:** Debe definirse antes de implementar el tramo de traslado hospitalario en qué momento y bajo qué reglas se selecciona el hospital y se actualiza su disponibilidad simulada.
- **Estado:** Aprobada (con reglas de selección de hospital y capacidad pendientes).
- **Referencias relacionadas:** `docs/product/requirements/business-rules.md` (`RN-HOS-001` a `RN-HOS-004`).

---

### DEC-015: Aprobación formal de la primera entrega documental (definición de producto y reglas de negocio)

- **Identificador:** DEC-015
- **Fecha:** 2026-11-12.
- **Título:** Aprobación formal de la primera entrega documental (`decision-log.md`, `vision-and-scope.md`, `stakeholders.md` y `business-rules.md`) tras auditoría de consistencia y correcciones autorizadas.
- **Contexto:** Tras completar la elaboración inicial de los cuatro artefactos de la primera entrega documental de la Fase 3, ejecutar la auditoría analítica de consistencia y aplicar las once correcciones autorizadas por el responsable humano, se sometió el conjunto documental a validación formal para establecer la línea base aceptada de definición del producto y sus reglas de negocio.
- **Decisión:**
  - Aprobar formalmente el contenido actual de:
    1. `docs/decisions/decision-log.md`
    2. `docs/product/vision-and-scope.md`
    3. `docs/product/stakeholders.md`
    4. `docs/product/requirements/business-rules.md`
    como la **versión inicial aceptada de la definición del producto y sus reglas de negocio**.
  - Establecer que esta aprobación **no significa que todos los parámetros funcionales, requisitos o decisiones técnicas estén definidos**, por lo que se conservan expresamente sin cerrar ni alterar su estado:
    - `DEC-001` a `DEC-014` como decisiones aprobadas.
    - `RN-ASI-005` y `RN-MET-003` como propuestas pendientes de aprobación.
    - Los parámetros, reglas y decisiones pendientes identificados en `PEN-002a` a `PEN-008` (`RN-EME-007`, `RN-CON-005`, `RN-RUT-006`, `RN-HOS-004`, `RN-MET-004`, el comportamiento de la cola al liberarse una ambulancia básica, versiones del stack y ADR pendientes).
    - Los aspectos de interfaz/permisos de actores listados como pendientes de definición en `docs/product/stakeholders.md` (Sección 5).
- **Justificación:** Consolida una base documental verificada, consistente y trazable para abordar posteriormente los requisitos funcionales y no funcionales sin convertir propuestas ni parámetros abiertos en compromisos aprobados prematuramente.
- **Consecuencias:**
  - **Positivas:** Los cuatro documentos pasan a estado aprobado en su versión inicial aceptada; se preserva intacta la distinción entre reglas aprobadas (37 en `business-rules.md`), propuestas pendientes de aprobación (`RN-ASI-005`, `RN-MET-003`) y parámetros/decisiones pendientes de definición (`PEN-002a` a `PEN-008`).
  - **Restricciones/Compromisos:** La aprobación de esta primera entrega documental no cierra `PEN-002a` a `PEN-008`, no autoriza aún el inicio de la siguiente entrega de requisitos funcionales/no funcionales sin instrucción expresa y no autoriza la implementación de código.
- **Estado:** Aprobada (conservando abiertos las propuestas y asuntos pendientes de definición).
- **Referencias relacionadas:** `docs/decisions/decision-log.md` (`DEC-001` a `DEC-014`, `PEN-002a` a `PEN-008`), `docs/product/vision-and-scope.md`, `docs/product/stakeholders.md`, `docs/product/requirements/business-rules.md`.

---

## 3. Decisiones y Definiciones Pendientes (No Aprobadas)

A continuación se listan los aspectos que **continúan pendientes de definición o aprobación humana**. Ninguno de ellos debe asumirse como cerrado:

| ID Temporal | Tema Pendiente | Descripción de qué decisión falta | Estado | Referencia |
| :--- | :--- | :--- | :--- | :--- |
| **PEN-001** | *(Resuelto por `DEC-008`)* Alcance del MVP vs. alcance total | Se conserva por trazabilidad histórica: quedó resuelto y aprobado mediante `DEC-008` (MVP equilibrado con los 8 componentes listados). | Cerrado por `DEC-008` | `DEC-008`, `docs/product/vision-and-scope.md` (Secc. 8) |
| **PEN-002a** | Catálogo y reglas concretas de Triage y Equipamiento | Definir niveles de clasificación, variables de entrada, categorías de ambulancia/equipamiento y reglas concretas iniciales del triage configurable. | Pendiente de definición | `DEC-010`, `business-rules.md` (`RN-EME-007`) |
| **PEN-002b** | Límite máximo de reintentos automáticos | Definir el número máximo de reintentos automáticos cuando una asignación concurrente falla porque la ambulancia dejó de estar disponible. | Pendiente de definición | `DEC-011`, `business-rules.md` (`RN-CON-005`) |
| **PEN-002c** | Reglas de selección de hospital y capacidad | Definir en qué momento del ciclo se selecciona el hospital destino, con qué criterio (cercanía, capacidad) y cómo se descuenta/libera su disponibilidad simulada. | Pendiente de definición | `DEC-014`, `business-rules.md` (`RN-HOS-004`) |
| **PEN-002d** | Cola ante liberación de unidad básica, desempate, estados, grafo y métricas | Definir el comportamiento de la cola cuando se libera una ambulancia básica, los criterios de desempate en asignación automática (`RN-ASI-005`), los nombres exactos de los estados del ciclo, los nodos/pesos del grafo simplificado de Arequipa (`RN-RUT-006`), la mecánica de avance temporal y el catálogo/auditoría de métricas experimentales (`RN-MET-003`, `RN-MET-004`). | Pendiente de definición | `DEC-007`, `DEC-012`, `DEC-013`, `business-rules.md` |
| **PEN-003** | Versiones exactas del stack tecnológico | Definir versiones concretas de Java, Spring Boot, MySQL y configuración de Maven antes de inicializar el repositorio de código. | Pendiente de aprobación | `AGENTS.md` (Secc. 2.2), `DEC-005` |
| **PEN-004** | Límites definitivos de módulos y ubicación de puertos | Validar la distribución final de módulos del monolito y la convención exacta de subpaquetes de puertos. | Pendiente de aprobación (requiere ADR) | `AGENTS.md` (Secc. 3.2, 3.3, 4.3) |
| **PEN-005** | Estrategia de persistencia, transacciones y migraciones | Definir generación de identificadores, mapeo de relaciones, estrategia de migraciones SQL, fronteras transaccionales e integridad referencial. | Pendiente de aprobación (requiere ADR) | `AGENTS.md` (Secc. 4.4) |
| **PEN-006** | Mecanismos técnicos de concurrencia en BD y aplicación | Evaluar y aprobar por operación crítica el uso de `@Version` (bloqueo optimista), bloqueo pesimista, niveles de aislamiento y ejecutores concurrentes. | Pendiente de aprobación (requiere ADR) | `AGENTS.md` (Secc. 6.3) |
| **PEN-007** | Configuración definitiva de herramientas de prueba | Confirmar configuración de JUnit 5, Mockito, Spring Boot Test, MockMvc y evaluar si se justifica Testcontainers con MySQL. | Pendiente de aprobación | `AGENTS.md` (Secc. 7.2) |
| **PEN-008** | Alineación de rutas documentales con `AGENTS.md` | `AGENTS.md` (Secciones 8.2 y 8.3) ilustra preliminarmente `docs/project/` y `docs/adr/`, mientras que la estructura autorizada en Fase 2 y Fase 3 utiliza `docs/product/` (`vision-and-scope.md`, `stakeholders.md`, `requirements/`) y `docs/decisions/` (`decision-log.md`, `adr/`). Falta autorizar la actualización de la Sección 8.2/8.3 de `AGENTS.md` para reflejar estas rutas definitivas. | Pendiente de autorización | `AGENTS.md` (Secc. 8.2, 8.3) |

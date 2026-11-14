# Bitácora de Decisiones — ARE (Arequipa RescUE)

**Estado del documento:** Aprobado — Línea base aceptada (`DEC-015`) y formalizaciones funcionales completas del MVP (`DEC-016` a `DEC-019`, cerrando `PEN-002a` a `PEN-002d`)
**Propósito:** Registrar de manera trazable las decisiones arquitectónicas, tecnológicas, funcionales y de producto formalmente acordadas en **ARE – Arequipa RescUE**, diferenciándolas estrictamente de las propuestas y definiciones que permanecen pendientes de aprobación.

> [!IMPORTANT]
> Solo llevan el estado **Aprobada** aquellas decisiones que cuentan con autorización expresa del responsable humano del proyecto. Las decisiones previas (`DEC-001` a `DEC-018`) se conservan íntegras y se amplían con `DEC-019` (que formaliza y cierra `PEN-002d`, incluyendo la aprobación acotada de `RN-ASI-005`, `RN-RUT-006`, `RN-MET-003` y `RN-MET-004`). Ningún asunto técnico o de alcance diferido (`PEN-003` a `PEN-008`, ni los aspectos abiertos de `stakeholders.md` Secc. 5) se registra como decisión aprobada.

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
| [DEC-016](#dec-016-formalización-de-pen-002a-sobre-modelo-de-clasificación-simulada-equipamiento-requerido-versionado-de-reglas-y-validaciones-de-registro) | Formalización de `PEN-002a` sobre modelo de clasificación simulada, equipamiento requerido, versionado de reglas y validaciones de registro | Dominio / Triage y Equipamiento | Aprobada (cierra `PEN-002a`; borradores en escenario activo pendientes) | 2026-11-13 |
| [DEC-017](#dec-017-formalización-de-pen-002b-sobre-política-de-reintentos-automáticos-ante-conflictos-concurrentes-registro-funcional-aviso-al-despachador-y-criterios-de-aceptación) | Formalización de `PEN-002b` sobre política de reintentos automáticos ante conflictos concurrentes, registro funcional, aviso al despachador y criterios de aceptación | Dominio / Despacho y Concurrencia | Aprobada (cierra `PEN-002b`; mecanismos técnicos en `PEN-006`) | 2026-11-13 |
| [DEC-018](#dec-018-formalización-de-pen-002c-sobre-selección-de-hospital-capacidad-numérica-simulada-reservas-concurrentes-cola-fifo-hospitalaria-y-alta-simulada) | Formalización de `PEN-002c` sobre selección de hospital, capacidad numérica simulada, reservas concurrentes, cola FIFO hospitalaria y alta simulada | Dominio / Hospitales y Concurrencia | Aprobada (cierra `PEN-002c`; alcances diferidos en `PEN-002d`, `PEN-005` y `PEN-006`) | 2026-11-13 |
| [DEC-019](#dec-019-formalización-de-pen-002d-sobre-cola-básica-desempate-estados-liberación-de-ambulancia-grafo-inicial-tiempo-simulado-por-eventos-discretos-y-métricas-mínimas) | Formalización de `PEN-002d` sobre cola básica, desempate, estados, liberación de ambulancia, grafo inicial, tiempo simulado por eventos discretos y métricas mínimas | Dominio / Simulación, Grafo, Estados y Métricas | Aprobada (cierra `PEN-002d`; pendientes técnicos en `PEN-003` a `PEN-008`) | 2026-11-13 |

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

### DEC-016: Formalización de PEN-002a sobre modelo de clasificación simulada, equipamiento requerido, versionado de reglas y validaciones de registro

- **Identificador:** DEC-016
- **Fecha:** 2026-11-13.
- **Origen de la aprobación humana:** Autorización expresa del responsable humano del proyecto ARE posterior a la línea base `DEC-015` (commit `cc1c190`) y al diagnóstico preliminar de ingeniería de requisitos, con el fin de cerrar el pendiente funcional `PEN-002a`.
- **Título:** Formalización del paquete funcional de `PEN-002a`: modelo determinista de clasificación por categorías ficticias, catálogo inicial, compatibilidad de equipamiento (`BASIC` / `SPECIALIZED`), versionado inmutable de reglas, campos del registro y validaciones.
- **Contexto:** `DEC-010` aprobó el uso de reglas configurables, reproducibles y trazables para la clasificación simulada de emergencias, pero dejó abierto en `PEN-002a` (`RN-EME-007`) el catálogo concreto de categorías de incidente, niveles de prioridad simulada, categorías de equipamiento, reglas de versionado, estructura de campos del registro de emergencia y validaciones funcionales, así como la restricción de activación de reglas durante escenarios activos relacionada con la Sección 5.2 de `docs/product/stakeholders.md`.
- **Alternativas consideradas:**
  1. **Alternativa A (Adoptada):** Modelo determinista basado en categorías ficticias de incidente (`INC-A`, `INC-B`, `INC-C`, `INC-D`), prioridad simulada exclusivamente informativa (`Alta`, `Media`, `Baja`), dos únicas categorías de equipamiento (`BASIC` y `SPECIALIZED`), reglas configurables con versiones publicadas inmutables, bloqueo de publicación/activación de reglas durante escenarios experimentales activos y validaciones estrictas de entrada sin clasificaciones por defecto.
  2. **Alternativa B (Descartada):** Modelo clínico multifactorial con evaluación de síntomas médicos o múltiples subcategorías de equipamiento individual y edición libre de reglas activas durante la ejecución de un escenario experimental. Se descartó porque infringe el límite académico no clínico (`DEC-001`, `DEC-010`) y compromete la reproducibilidad e integridad experimental de las corridas concurrentes.
- **Decisión:**
  1. **Modelo de clasificación determinista:** ARE utilizará un modelo determinista de clasificación mediante categorías ficticias y reglas configurables, consultando una versión activa de reglas para determinar la **prioridad simulada informativa** y la **categoría de equipamiento requerido**, sin algoritmos de diagnóstico clínico ni criterios médicos reales.
  2. **Catálogo inicial aprobado (exclusivamente académico):**
     - `INC-A`: Prioridad simulada `Alta` | Equipamiento requerido `Especializado` (`SPECIALIZED`).
     - `INC-B`: Prioridad simulada `Media` | Equipamiento requerido `Básico` (`BASIC`).
     - `INC-C`: Prioridad simulada `Baja` | Equipamiento requerido `Básico` (`BASIC`).
     - `INC-D`: Prioridad simulada `Alta` | Equipamiento requerido `Básico` (`BASIC`).
  3. **Independencia entre prioridad simulada y asignación:** La prioridad simulada es **únicamente informativa** y no altera el orden FIFO aprobado para las colas (`DEC-012`), la preservación de ambulancias especializadas (`DEC-011`), las condiciones de elegibilidad (`DEC-011`) ni el ordenamiento por costo de ruta mediante Dijkstra (`DEC-013`). El equipamiento requerido sí determina la compatibilidad de las ambulancias.
  4. **Compatibilidad de equipamiento:** Se establecen exclusivamente dos categorías iniciales sin crear categorías adicionales:
     - `BASIC`: equipamiento básico.
     - `SPECIALIZED`: equipamiento especializado, compatible también con necesidades básicas.
     Una ambulancia básica (`BASIC`) no puede satisfacer una necesidad especializada (`SPECIALIZED`). Una ambulancia especializada (`SPECIALIZED`) puede satisfacer una necesidad básica (`BASIC`), respetando las reglas ya aprobadas de preservación de unidades especializadas (`DEC-011`, `RN-PRE-001` a `RN-PRE-003`).
  5. **Versionado de reglas:**
     - Existirá una versión activa de reglas aplicable a los nuevos registros.
     - Cada emergencia conservará la referencia a la versión que produjo su clasificación.
     - Las versiones publicadas serán inmutables; cualquier cambio se incorporará mediante una nueva versión y su publicación no modificará retroactivamente las emergencias ya clasificadas.
     - Durante la ejecución de un escenario experimental activo no se permitirá publicar ni activar cambios de reglas.
     - La preparación de borradores durante escenarios activos no queda aprobada ni prohibida por esta decisión; permanece pendiente de definición.
  6. **Campos del registro de emergencia:**
     - *Introducidos por el Operador de recepción (`ACT-01`):* Categoría del incidente (obligatoria), Nodo de ubicación en el grafo (obligatorio) y Descripción ficticia (opcional).
     - *Generados o determinados automáticamente:* Identificador único de emergencia, Marca temporal de registro, Prioridad simulada, Equipamiento requerido e Identificador o referencia de versión de reglas aplicada.
  7. **Validaciones aprobadas:**
     - Rechazar registros sin categoría, con categorías inexistentes o inactivas, o con ubicaciones que no correspondan a nodos válidos del grafo configurado.
     - Permitir descripción vacía.
     - Exigir una regla válida y no ambigua para clasificar la categoría, sin generar clasificaciones por defecto cuando falte una regla válida.
     - Impedir la publicación de versiones con categorías duplicadas, incompletas o reglas contradictorias.
     - Mantener la referencia histórica a la versión aplicada.
     - Si no existen ambulancias elegibles, conservar la emergencia registrada y pasarla a estado pendiente (`DEC-011`, `RN-ASI-004`); una falla de asignación por falta de ambulancias no equivale a una falla del registro de emergencia.
  8. **Consistencia funcional:** Para una misma categoría y versión de reglas, el resultado de clasificación debe ser determinista, y cada emergencia debe quedar asociada a una única versión consistente incluso cuando existan operaciones concurrentes.
- **Justificación:** Cierra de manera completa y verificable la definición funcional del triage simulado y de la compatibilidad de equipamiento en total coherencia con `DEC-010`, `DEC-011` y `DEC-012`, garantizando determinismo e inmutabilidad histórica en escenarios concurrentes sin introducir criterios médicos reales.
- **Consecuencias:**
  - **Positivas:** Queda cerrado `PEN-002a` y se formalizan las reglas `RN-EME-007` a `RN-EME-012` en `business-rules.md`; `INC-D` (`Alta` / `BASIC`) frente a `INC-A` (`Alta` / `SPECIALIZED`) permite demostrar empíricamente que la compatibilidad depende del equipamiento requerido y no de la etiqueta informativa de prioridad.
  - **Restricciones/Compromisos:** No se deciden todavía el formato físico de los identificadores, tipos SQL, precisión temporal, catálogo concreto de nodos del grafo ni el mecanismo técnico de persistencia, bloqueo o activación atómica de versiones (que corresponden a `PEN-002d`, `PEN-005` y `PEN-006`). Tampoco se aprueba ni prohíbe aún la preparación de borradores de reglas durante escenarios activos.
- **Estado:** Aprobada.
- **Referencias relacionadas:** `DEC-010`, `DEC-011`, `DEC-012`, `DEC-015`, `PEN-002a`, `docs/product/requirements/business-rules.md` (`RN-EME-007` a `RN-EME-012`), `docs/product/vision-and-scope.md`, `docs/product/stakeholders.md`.

---

### DEC-017: Formalización de PEN-002b sobre política de reintentos automáticos ante conflictos concurrentes, registro funcional, aviso al despachador y criterios de aceptación

- **Identificador:** DEC-017
- **Fecha:** 2026-11-13.
- **Origen de la aprobación humana:** Autorización expresa del responsable humano del proyecto ARE posterior a `DEC-016` (commit `1cf5ae9`) para formalizar y cerrar el pendiente funcional `PEN-002b`.
- **Título:** Formalización del comportamiento funcional de reintentos automáticos ante conflictos concurrentes de asignación de ambulancias (`PEN-002b`), incluyendo límite de intentos, reevaluación obligatoria, finalización anticipada, tratamiento del agotamiento, registro funcional, aviso interno al despachador, casos límite y criterios de aceptación (`CA-CON-01` a `CA-CON-10`).
- **Contexto:** `DEC-011` estableció que cuando una asignación automática falla debido a que la ambulancia seleccionada dejó de estar disponible por una operación concurrente, el sistema reevalúa alternativas y reintenta automáticamente un número limitado de veces, permaneciendo la emergencia en estado pendiente si se agotan los reintentos. Quedaba pendiente en `PEN-002b` (`RN-CON-005`) fijar el número máximo de reintentos automáticos, las condiciones exactas de disparo y terminación anticipada, la información funcional a registrar, la notificación al `Despachador` y los casos límite y criterios de aceptación verificables, sin anticipar las decisiones técnicas de bloqueo o aislamiento correspondientes a `PEN-006`.
- **Alternativas consideradas:**
  1. **Alternativa A (Adoptada):** Política acotada a conflictos concurrentes en asignación automática con **1 intento inicial + hasta 3 reintentos automáticos adicionales (máximo 4 intentos totales)** por operación independiente, reevaluación integral obligatoria tras cada conflicto, detención inmediata ante éxito o ausencia sobrevenida de elegibles (sin consumir reintentos restantes), permanencia en estado `pendiente` al agotarse el límite, registro funcional del resultado (distinguiendo el motivo funcional como `CONCURRENCY_RETRY_EXHAUSTED` frente a falta de elegibles y errores técnicos) y aviso visible al `Despachador` en la interfaz web interna, sin extender el límite de 3 reintentos a la modalidad supervisada.
  2. **Alternativa B (Descartada):** Reintentos ilimitados o reintentos ciegos sobre la misma unidad sin reevaluar elegibilidad, equipamiento, preservación y costo por Dijkstra, o bien consumir reintentos de concurrencia cuando inicialmente no existen ambulancias elegibles. Se descartó porque impediría la terminación determinista de la operación bajo alta contención, violaría las reglas de elegibilidad y preservación (`DEC-011`, `DEC-016`) y confundiría la escasez normal de flota con un conflicto de concurrencia.
- **Decisión:**
  1. **Alcance funcional de la política de reintentos:**
     - Aplica exclusivamente a las operaciones de **asignación automática** de ambulancias cuando ocurre un **conflicto concurrente** (otra operación concurrente modificó o comprometió la disponibilidad de la ambulancia seleccionada antes de que la asignación actual pudiera confirmarse).
     - No debe confundirse con: (a) ausencia normal de ambulancias elegibles ni (b) errores técnicos generales del sistema.
  2. **Límite máximo de intentos:**
     - Cada operación de asignación automática tendrá **1 intento inicial** y **hasta 3 reintentos automáticos adicionales**, lo que equivale a un **máximo de 4 intentos totales** dentro de una misma operación de asignación automática.
     - Los reintentos adicionales solo se activan cuando un intento falla por conflicto concurrente de disponibilidad. No se permiten reintentos ilimitados.
  3. **Reevaluación obligatoria en cada reintento:**
     - Después de cada conflicto concurrente, y antes de intentar una nueva asignación, el sistema debe obligatoriamente:
       1. Consultar nuevamente la disponibilidad de ambulancias.
       2. Reevaluar las condiciones de elegibilidad (`RN-ELE-002`).
       3. Respetar la compatibilidad de equipamiento (`BASIC` y `SPECIALIZED`, `RN-EME-007`).
       4. Aplicar la preservación de ambulancias especializadas (`RN-PRE-001` a `RN-PRE-003`).
       5. Recalcular la selección según el costo de ruta mediante Dijkstra y las reglas vigentes (`RN-ELE-003`, `RN-RUT-002`).
     - No se permite reintentar a ciegas sobre una ambulancia que ya dejó de estar disponible.
  4. **Finalización anticipada:**
     - El proceso de reintentos termina inmediatamente cuando ocurre cualquiera de estas condiciones:
       1. Se confirma una asignación válida.
       2. Tras una reevaluación ya no existen ambulancias elegibles disponibles; en este caso la emergencia permanece en estado `pendiente` y **no se siguen consumiendo reintentos vacíos**.
     - Si desde el inicio no existen ambulancias elegibles, la emergencia pasa o permanece en estado `pendiente` y se consumen `0` reintentos de concurrencia.
  5. **Tratamiento del agotamiento del límite:**
     - Si el intento inicial y los 3 reintentos adicionales fallan por conflictos concurrentes:
       1. La emergencia permanece registrada y en estado `pendiente`.
       2. Se registra como motivo el agotamiento de reintentos por conflicto concurrente.
       3. Se notifica al `Despachador` (`ACT-02`) mediante un aviso visible en la interfaz web de ARE.
       4. No se ejecutan más reintentos dentro de esa misma operación.
     - La emergencia nunca se cancela ni se elimina por este motivo, y no se introducen servicios externos de notificación como correo electrónico o SMS.
  6. **Registro funcional del resultado:**
     - Cuando una operación automática requiera reintentos o termine sin asignación por conflicto concurrente, el sistema debe conservar al menos la siguiente información funcional:
       1. Identificador de la emergencia.
       2. Resultado de la operación.
       3. Motivo del resultado, distinguiendo el agotamiento de reintentos por concurrencia (por ejemplo, código funcional `CONCURRENCY_RETRY_EXHAUSTED`) de la falta de ambulancias elegibles y de errores técnicos.
       4. Número de intentos ejecutados.
       5. Número de reintentos adicionales consumidos.
       6. Tipo de operación (`automática`).
       7. Marca temporal del evento.
     - *(No se fija en esta etapa el esquema SQL ni la estructura técnica definitiva).*
  7. **Notificación interna al Despachador:**
     - El `Despachador` (`ACT-02`) debe poder identificar desde la interfaz del sistema:
       1. La emergencia que quedó en estado `pendiente`.
       2. El motivo de agotamiento de reintentos por concurrencia.
       3. Que la operación automática terminó sin asignación.
     - *(No se fija todavía el componente visual específico, el mecanismo de refresco de pantalla ni la tecnología de notificación interna).*
  8. **Consistencia y exclusividad:**
     - Una ambulancia nunca puede quedar asignada simultáneamente a dos emergencias incompatibles (`RN-CON-001`).
     - Ningún intento fallido puede dejar asignaciones parciales confirmadas.
     - Una asignación exitosa detiene inmediatamente cualquier reintento adicional.
     - Los reintentos no alteran el orden FIFO de las colas (`DEC-012`), las reglas de elegibilidad (`DEC-011`), la preservación de ambulancias especializadas (`DEC-011`) ni el cálculo de rutas mediante Dijkstra (`DEC-013`).
     - Cada operación de asignación independiente maneja su propio contador de intentos; si posteriormente se inicia una nueva operación independiente de asignación para la emergencia pendiente (por ejemplo, al liberarse una ambulancia o ejecutarse un nuevo ciclo de atención de cola), esa nueva operación inicia su propio contador desde cero.
  9. **Asignación supervisada:**
     - Se conserva sin cambios el comportamiento aprobado en `DEC-011` y `RN-CON-002`: cuando el `Despachador` selecciona manualmente una ambulancia en modo supervisado, el sistema vuelve a validar su disponibilidad y elegibilidad antes de confirmar; si dejó de estar disponible por concurrencia, la asignación no se confirma y la interfaz actualiza las alternativas elegibles para que el `Despachador` realice una nueva selección.
     - **No se extiende el límite de 3 reintentos automáticos al modo supervisado.**
  10. **Casos límite y criterios de aceptación:**
      - Se aprueban e incorporan en `docs/product/requirements/business-rules.md` los diez casos límite funcionales y los diez criterios de aceptación verificables `CA-CON-01` a `CA-CON-10`.
- **Justificación:** Proporciona una especificación funcional completa, determinista y auditable del manejo de conflictos concurrentes en asignación automática, diferenciando con precisión los fallos de concurrencia frente a la falta de flota elegible o errores técnicos, garantizando observabilidad para el `Despachador` y preservando intactas las invariantes de elegibilidad, preservación, FIFO y Dijkstra.
- **Consecuencias:**
  - **Positivas:** Queda cerrado `PEN-002b`; se actualiza `RN-CON-005` a estado **Aprobada** y se formalizan las reglas complementarias `RN-CON-006` a `RN-CON-009` junto con `CA-CON-01` a `CA-CON-10` en `business-rules.md`.
  - **Restricciones/Compromisos:** Esta decisión no resuelve `PEN-006`; no se definen todavía bloqueo optimista (`@Version`) o pesimista, niveles de aislamiento transaccional, mecanismos técnicos de retry de Spring, restricciones SQL, manejo técnico de rollback, hilos/ejecutores, WebSockets/polling ni la implementación de pruebas concurrentes.
- **Estado:** Aprobada.
- **Referencias relacionadas:** `DEC-009`, `DEC-011`, `DEC-012`, `DEC-013`, `DEC-015`, `DEC-016`, `PEN-002b`, `PEN-006`, `docs/product/requirements/business-rules.md` (`RN-CON-001` a `RN-CON-009`, `CA-CON-01` a `CA-CON-10`), `docs/product/vision-and-scope.md`, `docs/product/stakeholders.md`.

---

### DEC-018: Formalización de PEN-002c sobre selección de hospital, capacidad numérica simulada, reservas concurrentes, cola FIFO hospitalaria y alta simulada

- **Identificador:** DEC-018
- **Fecha:** 2026-11-13.
- **Origen de la aprobación humana:** Autorización expresa del responsable humano del proyecto ARE posterior a `DEC-017` (commit `cca8f31`) para formalizar y cerrar el pendiente funcional `PEN-002c`.
- **Título:** Formalización del modelo funcional de capacidad numérica simulada de hospitales, selección automática tras la recogida, reserva concurrente con reintentos acotados, espera de destino con cola FIFO, llegada con transformación en ocupación, alta simulada e independencia de liberación de recursos (`PEN-002c`).
- **Contexto y problema abordado:** `DEC-014` aprobó la gestión básica de hospitales y estableció que la disponibilidad hospitalaria no interviene en la selección inicial de ambulancias (`RN-ELE-004`, `RN-HOS-002`), dejando abierto en `PEN-002c` (`RN-HOS-004`) cómo se representa la capacidad hospitalaria simulada, en qué momento del ciclo se selecciona el hospital de destino, bajo qué criterios de elegibilidad, costo y desempate se elige, cómo se gestionan las reservas concurrentes y la saturación temporal de plazas, cómo se transforma la reserva en ocupación al llegar al hospital y cómo se liberan las plazas ocupadas sin simular procesos clínicos reales (`DEC-001`, `DEC-007`).
- **Alternativas consideradas:**
  1. **Alternativa A (Descartada — Disponibilidad binaria):** Representar a cada hospital únicamente con un indicador booleano (`disponible` / `no disponible`) sin conteo explícito de plazas ni reservas. Se descartó porque impide modelar y verificar condiciones de carrera reales por la última plaza disponible, no distingue entre plazas reservadas en tránsito y plazas efectivamente ocupadas tras la llegada, y limita el valor académico de las pruebas de concurrencia y de saturación hospitalaria.
  2. **Alternativa B (Adoptada — Capacidad numérica simplificada con reservas y ocupaciones):** Modelar cada hospital mediante cuatro magnitudes enteras no negativas de plazas ficticias de simulación —capacidad total `C`, plazas reservadas `R`, plazas ocupadas `O` y plazas disponibles `D = C - R - O` con la invariante `R + O <= C`—, iniciando la selección y reserva de plaza al confirmar la recogida de la emergencia por la ambulancia, aplicando Dijkstra con desempate determinista, permitiendo 1 intento inicial + hasta 2 reintentos ante conflictos concurrentes (máximo 3 intentos totales), gestionando una cola FIFO de espera de destino cuando no hay hospitales elegibles, transformando la reserva en ocupación al llegar al hospital y liberando la plaza ocupada mediante un evento explícito de alta simulada.
- **Justificación de la capacidad numérica:** La capacidad numérica simplificada (`C`, `R`, `O`, `D`) mantiene el nivel de realismo intermedio aprobado en `DEC-007` sin introducir complejidad clínica intrahospitalaria (`DEC-001`), y a la vez provee un recurso cuantificable y escaso sobre el cual demostrar empíricamente invariantes de no sobreocupación, prevención de doble reserva, competencia por la última plaza, reintentos acotados y recuperación ordenada mediante cola FIFO.
- **Decisión funcional completa:**
  1. **Capacidad hospitalaria simulada e invariantes (`C`, `R`, `O`, `D`):**
     - Cada hospital tendrá cuatro cantidades enteras no negativas de plazas ficticias de simulación (no camas clínicas reales):
       - Capacidad total `C`.
       - Plazas reservadas `R`.
       - Plazas ocupadas `O`.
       - Plazas disponibles `D = C - R - O`.
     - Invariantes obligatorias:
       - `R + O <= C` (no puede existir sobreocupación).
       - Una plaza no puede reservarse simultáneamente para dos emergencias.
       - Una emergencia no puede mantener dos reservas hospitalarias activas.
       - Una reserva y una ocupación no deben contabilizarse simultáneamente para la misma plaza y emergencia.
  2. **Momento y criterios de selección del hospital:**
     - La selección del hospital comienza **al confirmar la recogida de la emergencia por la ambulancia** (manteniendo intacto `DEC-014`: no interviene en la selección inicial de ambulancias).
     - Solo son elegibles los hospitales que cumplan simultáneamente: (a) estar habilitados, (b) ser alcanzables mediante el grafo simulado y (c) contar con al menos una plaza disponible (`D >= 1`).
     - ARE calcula el costo de ruta desde el nodo del incidente hasta cada hospital elegible utilizando el algoritmo de **Dijkstra** (`DEC-013`).
     - Se selecciona el hospital elegible con **menor costo de ruta**.
     - Si dos o más hospitales elegibles tienen exactamente el mismo costo mínimo de ruta, se aplica un **orden determinista mediante su identificador técnico estable**.
     - El destino hospitalario solo queda confirmado cuando se confirma también su reserva de plaza; **no se permite iniciar el traslado hacia el hospital sin destino confirmado**.
  3. **Reserva concurrente y política de reintentos hospitalarios:**
     - Cada operación independiente de reserva hospitalaria permite **1 intento inicial** y **hasta 2 reintentos adicionales exclusivamente ante conflictos concurrentes**, lo que equivale a un **máximo de 3 intentos totales**.
     - En cada reintento el sistema debe reevaluar obligatoriamente: disponibilidad actual, capacidad disponible (`D`), elegibilidad hospitalaria, alcanzabilidad en el grafo, costo de ruta (Dijkstra) y desempate determinista.
     - La operación finaliza inmediatamente cuando se confirma una reserva válida o cuando ya no quedan hospitales elegibles (sin consumir reintentos vacíos).
     - Si no existen hospitales elegibles desde el intento inicial, la emergencia pasa a espera de destino consumiendo `0` reintentos por concurrencia.
     - Si se agotan el intento inicial y los 2 reintentos adicionales por conflictos concurrentes, la emergencia pasa a espera de destino registrando un motivo funcional distinguible (por ejemplo, `HOSPITAL_RESERVATION_RETRY_EXHAUSTED`).
     - Los errores técnicos generales no deben clasificarse automáticamente como conflictos concurrentes.
     - Esta política es **independiente de `DEC-017`**: no modifica el límite de reintentos para la asignación automática de ambulancias (1 intento inicial + hasta 3 reintentos = máximo 4 intentos).
  4. **Espera de destino hospitalario:**
     - Cuando no exista hospital elegible o no se logre confirmar una reserva tras agotar los reintentos concurrentes:
       1. La emergencia permanece activa.
       2. Conserva la ambulancia asignada y ocupada.
       3. No inicia el traslado hacia el hospital.
       4. Entra en **espera de destino hospitalario**.
       5. Se registra el motivo funcional correspondiente.
     - Esta condición no debe confundirse con la espera de asignación de ambulancia (`RN-ASI-004`). El nombre definitivo del estado se resolverá junto con la máquina de estados en `PEN-002d`.
  5. **Cola FIFO hospitalaria y reevaluación automática:**
     - Las emergencias en espera de destino hospitalario se ordenan según el momento de ingreso a dicha espera (disciplina FIFO); ante igualdad de marca temporal, se aplica un desempate determinista mediante identificador estable.
     - Cuando se libera capacidad hospitalaria, ARE activa una reevaluación automática respetando el orden FIFO.
     - Si la primera emergencia de la cola no tiene ningún hospital elegible (por ejemplo, por alcanzabilidad), permanece en espera conservando su posición original y se permite evaluar la siguiente emergencia; una reevaluación fallida nunca desplaza automáticamente una emergencia al final de la cola.
     - Cada nueva operación independiente de reserva utiliza su propio contador de intentos hospitalarios (hasta 3 intentos totales), sin acumular los intentos de operaciones anteriores.
  6. **Llegada al hospital y transformación en ocupación:**
     - Cuando la ambulancia llega al hospital con una reserva confirmada:
       1. La reserva se transforma en ocupación.
       2. Se reduce `R` en una unidad (`R = R - 1`).
       3. Se incrementa `O` en una unidad (`O = O + 1`).
       4. La disponibilidad `D = C - R - O` permanece sin cambios.
     - La llegada no consume una segunda plaza, y la reserva confirmada permanece válida aunque el hospital alcance `D = 0` posteriormente por otras emergencias.
  7. **Alta simulada:**
     - La plaza ocupada (`O`) se libera mediante un **evento explícito de alta simulada**, activado por un operador autorizado (cuyos permisos exactos quedan pendientes de definición en la matriz de responsabilidades y permisos).
     - El alta simulada requiere una ocupación válida, libera exactamente una plaza ocupada (`O = O - 1`), incrementa la disponibilidad (`D = D + 1`), no puede ejecutarse dos veces con efecto sobre la misma ocupación (idempotencia/rechazo de duplicados) y desencadena la reevaluación automática de las emergencias en espera de destino hospitalario.
     - No representa un alta médica real (`DEC-001`, `DEC-007`).
  8. **Independencia de recursos (plaza hospitalaria vs. ambulancia):**
     - La liberación de una plaza hospitalaria mediante alta simulada no implica la liberación de una ambulancia, y el momento exacto de liberación de la ambulancia permanece pendiente de definición en `PEN-002d`. No se aprueban estados definitivos de ambulancias ni se modifica su ciclo de vida mediante esta decisión.
  9. **Restricción de edición de capacidad hospitalaria:**
     - Se prohíbe modificar la capacidad hospitalaria (`C`) durante la ejecución de escenarios experimentales activos.
     - Fuera de escenarios experimentales activos, cualquier modificación de capacidad debe preservar las invariantes (`R + O <= C`, `D = C - R - O >= 0`).
     - No se extiende automáticamente esta restricción a otros catálogos cuyo comportamiento durante escenarios activos continúa pendiente (`ambulancias`).
  10. **Reglas y criterios de aceptación:**
      - Se actualizan y formalizan en `docs/product/requirements/business-rules.md` las reglas `RN-HOS-004` a `RN-HOS-015` y los trece criterios de aceptación verificables `CA-HOS-01` a `CA-HOS-13`.
- **Consecuencias:**
  - **Positivas:** Queda cerrado `PEN-002c` con un modelo cuantitativo determinista y libre de contradicciones con `DEC-014`, `DEC-016` y `DEC-017`; se habilita la verificación de concurrencia tanto en despacho de ambulancias como en reserva de plazas hospitalarias.
  - **Negativas / Compromisos (Dependencias y alcance diferido):**
    - Requiere coordinar el estado de espera de destino hospitalario y el momento exacto de liberación de la ambulancia cuando se resuelva `PEN-002d`.
    - Se mantienen explícitamente pendientes y fuera de esta decisión (en `PEN-002d`, `PEN-005`, `PEN-006` y Sección 5 de `stakeholders.md`): la cancelación de reservas confirmadas, la reasignación de destinos ya reservados, los permisos exactos del actor para activar altas simuladas, los nombres definitivos de los estados de emergencia y ambulancia, el momento exacto de liberación de ambulancias, los mecanismos técnicos de concurrencia/persistencia (bloqueos, aislamiento, transacciones, restricciones SQL, retry técnico y ejecución concurrente de la cola en `PEN-005`/`PEN-006`) y la configuración temporal del motor de simulación (`PEN-002d`).
- **Estado:** Aprobada (cierra `PEN-002c`).
- **Referencias relacionadas:** `DEC-001`, `DEC-007`, `DEC-008`, `DEC-009`, `DEC-013`, `DEC-014`, `DEC-015`, `DEC-016`, `DEC-017`, `PEN-002c`, `PEN-002d`, `PEN-005`, `PEN-006`, `docs/product/requirements/business-rules.md` (`RN-HOS-001` a `RN-HOS-015`, `CA-HOS-01` a `CA-HOS-13`), `docs/product/vision-and-scope.md`, `docs/product/stakeholders.md`.

---

### DEC-019: Formalización de PEN-002d sobre cola básica, desempate, estados, liberación de ambulancia, grafo inicial, tiempo simulado por eventos discretos y métricas mínimas

- **Identificador:** DEC-019
- **Fecha:** 2026-11-13.
- **Origen de la aprobación humana:** Autorización expresa del responsable humano del proyecto ARE posterior a `DEC-018` (commit `9fbb6e9`) para cerrar `PEN-002d` mediante un único paquete funcional orientado a un MVP académico simplificado.
- **Título:** Formalización del paquete funcional de `PEN-002d`: atención de cola al liberarse ambulancia `BASIC`, desempate determinista de ambulancias (`RN-ASI-005`), máquina mínima de estados de emergencia y ambulancia, liberación de ambulancia a la llegada al hospital, grafo inicial de 10 nodos inspirado en Arequipa (`RN-RUT-006`), simulación por eventos discretos con avance manual y catálogo mínimo de métricas y registro experimental (`RN-MET-003`, `RN-MET-004`).
- **Contexto y problema abordado:** Tras cerrar `PEN-002a` (`DEC-016`), `PEN-002b` (`DEC-017`) y `PEN-002c` (`DEC-018`), permanecían abiertos en `PEN-002d` ocho elementos funcionales indispensables para completar la línea base de reglas de negocio antes de pasar a la ingeniería de requisitos detallados: (1) comportamiento de la cola al liberarse una ambulancia `BASIC`, (2) regla de desempate entre ambulancias elegibles de igual costo (`RN-ASI-005`), (3) estados funcionales mínimos y transiciones de emergencia y ambulancia, (4) momento exacto de liberación de la ambulancia e independencia frente al alta simulada hospitalaria, (5) configuración inicial del grafo simplificado de Arequipa (`RN-RUT-006`), (6) modelo de avance del tiempo simulado, (7) catálogo mínimo de métricas experimentales (`RN-MET-004`) y (8) alcance del registro experimental mínimo (`RN-MET-003`).
- **Alternativas consideradas:**
  1. **Alternativa A (Adoptada — MVP académico simplificado y determinista):** Cerrar `PEN-002d` con una política FIFO exclusiva para emergencias `BASIC` al liberarse una ambulancia `BASIC`, desempate determinista por identificador técnico estable, conjunto mínimo de 7 estados de emergencia y 4 estados de ambulancia, liberación de ambulancia al completar la llegada y entrega simulada en el hospital, grafo ponderado ficticio de 10 nodos (9 conectados con rutas alternativas/empates y 1 aislado para pruebas de inalcanzabilidad), simulación por eventos discretos con avance manual desde la interfaz web y tiempo lógico monotónico, y un catálogo acotado de 7 métricas con registro experimental trazable.
  2. **Alternativa B (Descartada — Simulación continua en tiempo real, grafo cartográfico extenso y subestados granulares):** Implementar reloj acelerado con hilos de avance continuo en segundo plano, decenas de nodos georreferenciados y plataformas de auditoría o analítica compleja. Se descartó porque introduce no determinismo temporal que dificulta reproducir pruebas académicas, excede el nivel de realismo intermedio (`DEC-007`, `DEC-008`) y desvía el foco respecto a la arquitectura hexagonal y el control de concurrencia.
- **Decisión funcional completa:**
  1. **Atención de la cola al liberarse una ambulancia (`RN-COL-007`):**
     - **Al liberarse una ambulancia `BASIC`:** el sistema evalúa únicamente las emergencias pendientes (`WAITING_AMBULANCE`) que requieren equipamiento `BASIC`, respetando estricto orden FIFO según el momento de ingreso a espera (y desempate por identificador técnico estable ante igualdad temporal), y selecciona la emergencia elegible y alcanzable más antigua; si no existe emergencia compatible, la ambulancia permanece en estado `AVAILABLE`.
     - **Al liberarse una ambulancia `SPECIALIZED`:** se mantienen intactas las reglas aprobadas (`DEC-012`, `RN-COL-001` a `RN-COL-003`), evaluando primero las emergencias pendientes que requieren `SPECIALIZED` y, solo si no existen, las que requieren `BASIC`, respetando FIFO dentro de cada grupo.
     - La prioridad simulada informativa (`Alta`, `Media`, `Baja`) no altera ninguno de estos órdenes (`DEC-016`, `RN-EME-008`).
  2. **Desempate determinista de ambulancias (`RN-ASI-005` — Aprobada):**
     - Cuando dos o más ambulancias elegibles del mismo grupo de preferencia presenten exactamente el mismo costo mínimo de ruta (Dijkstra) hacia el nodo del incidente, el sistema selecciona la unidad de **menor identificador técnico estable** según orden determinista ascendente.
  3. **Estados funcionales mínimos y transiciones (`RN-RUT-007`):**
     - **Estados de emergencia (7):** `REGISTERED`, `WAITING_AMBULANCE`, `ASSIGNED`, `AT_INCIDENT`, `WAITING_HOSPITAL`, `IN_TRANSPORT`, `COMPLETED`.
     - **Estados de ambulancia (4):** `AVAILABLE`, `ASSIGNED`, `AT_INCIDENT`, `TRANSPORTING`.
     - La condición `WAITING_HOSPITAL` conserva la ambulancia asignada y ocupada en `AT_INCIDENT` sin iniciar traslado (`DEC-018`, `RN-HOS-009`). No se introducen estados adicionales, cancelación de emergencias ni reasignación de reservas hospitalarias confirmadas.
  4. **Liberación de ambulancia (`RN-RUT-008`, `RN-HOS-014`):**
     - La ambulancia se libera (transición `TRANSPORTING -> AVAILABLE`) **al confirmar la llegada al hospital y completar la entrega simulada** (transición de la emergencia `IN_TRANSPORT -> COMPLETED`), disparando la evaluación de la cola de emergencias `WAITING_AMBULANCE` según su categoría (`BASIC` o `SPECIALIZED`).
     - Esta liberación es independiente del alta simulada hospitalaria: la plaza hospitalaria permanece ocupada (`O`) hasta que se ejecute el evento explícito de alta simulada aprobado en `DEC-018` (`RN-HOS-013`). No se inventan tiempos clínicos ni procesos de tratamiento intrahospitalario.
  5. **Grafo simplificado inicial de Arequipa (`RN-RUT-006` — Aprobada):**
     - Se adopta un grafo ficticio, no dirigido y ponderado inspirado en zonas de Arequipa con **10 nodos** (`N01` a `N10`) como configuración inicial del MVP, donde los pesos de arista son enteros positivos expresados en unidades abstractas de costo (no distancias ni tiempos reales).
     - Los nodos `N01` a `N09` conforman una red conexa con rutas alternativas y empates de costo para pruebas de Dijkstra y desempate determinista, mientras que `N10` (`YURA_AISLADO`) actúa como nodo sin conexión activa hacia la componente principal para verificar determinísticamente los casos de inalcanzabilidad (`CA-HOS-03`, `CA-HOS-09`).
     - No se integra GPS, Google Maps, tráfico en tiempo real ni geolocalización real (`DEC-008`).
  6. **Tiempo simulado por eventos discretos con avance manual (`RN-RUT-009`):**
     - Se adopta **simulación por eventos discretos con avance manual** desde la interfaz web, permitiendo avanzar paso a paso al siguiente evento pendiente.
     - El motor mantiene un **tiempo lógico monotónico no negativo** ($t \ge 0$, en unidades lógicas abstractas) y un orden determinista de ejecución de eventos; ante igualdad de tiempo lógico, los eventos se ordenan por tipo de evento según precedencia operativa determinista y luego por secuencia monotónica de creación / identificador técnico estable.
     - No se implementa reloj acelerado, procesos en tiempo real ni simulación continua en segundo plano.
     - La clasificación, el orden FIFO y los registros utilizan marcas de tiempo consistentes con este modelo simulado, sin confundir el avance del tiempo lógico con la ejecución concurrente real de operaciones de asignación o reserva.
  7. **Catálogo mínimo de métricas (`RN-MET-004` — Aprobada) y registro experimental (`RN-MET-003` — Aprobada):**
     - Se aprueba el catálogo reducido de **7 métricas**: (1) tiempo de espera hasta asignación de ambulancia, (2) tiempo de respuesta hasta llegada al incidente, (3) tiempo desde recogida hasta llegada al hospital, (4) número de emergencias completadas, (5) utilización de ambulancias, (6) número de conflictos concurrentes y reintentos (desglosando asignación de ambulancia y reserva hospitalaria) y (7) tiempo de espera por destino hospitalario, especificando inicio, fin, unidad y tratamiento de mediciones aún en curso.
     - Se aprueba el alcance mínimo de `RN-MET-003` para conservar un registro funcional trazable (ID de emergencia, eventos principales, tiempo lógico, ambulancia y hospital asignados, resultados de asignación y reserva, conflictos/reintentos y versión de reglas aplicada), sin crear una plataforma de auditoría independiente.
- **Consecuencias:**
  - **Positivas:** Queda cerrado `PEN-002d`, completando el cierre de todos los pendientes funcionales de reglas de negocio (`PEN-001` y `PEN-002a` a `PEN-002d`) y dejando 100 % aprobadas las reglas de los nueve temas de `business-rules.md`.
  - **Restricciones/Compromisos:** Permanecen explícitamente fuera de esta decisión y abiertos: los mecanismos técnicos de concurrencia (`PEN-006`), la estrategia de persistencia, transacciones y migraciones (`PEN-005`), las versiones exactas del stack (`PEN-003`), los límites definitivos de módulos y puertos (`PEN-004`), las herramientas de prueba (`PEN-007`), la alineación de rutas documentales en `AGENTS.md` (`PEN-008`), los permisos exactos para activar el alta simulada, la cancelación/reasignación de reservas hospitalarias confirmadas y la edición de otros catálogos / preparación de borradores durante escenarios activos (`stakeholders.md` Secc. 5).
- **Estado:** Aprobada (cierra `PEN-002d`).
- **Referencias relacionadas:** `DEC-001`, `DEC-007`, `DEC-008`, `DEC-009`, `DEC-010`, `DEC-011`, `DEC-012`, `DEC-013`, `DEC-014`, `DEC-015`, `DEC-016`, `DEC-017`, `DEC-018`, `PEN-002d`, `docs/product/requirements/business-rules.md`, `docs/product/vision-and-scope.md`, `docs/product/stakeholders.md`.

---

## 3. Decisiones y Definiciones Pendientes (No Aprobadas)

A continuación se listan los aspectos que **continúan pendientes de definición o aprobación humana**. Ninguno de ellos debe asumirse como cerrado:

| ID Temporal | Tema Pendiente | Descripción de qué decisión falta | Estado | Referencia |
| :--- | :--- | :--- | :--- | :--- |
| **PEN-001** | *(Resuelto por `DEC-008`)* Alcance del MVP vs. alcance total | Se conserva por trazabilidad histórica: quedó resuelto y aprobado mediante `DEC-008` (MVP equilibrado con los 8 componentes listados). | Cerrado por `DEC-008` | `DEC-008`, `docs/product/vision-and-scope.md` (Secc. 8) |
| **PEN-002a** | *(Resuelto por `DEC-016`)* Catálogo y reglas concretas de Triage y Equipamiento | Quedó formalizado mediante `DEC-016` (modelo determinista, catálogo inicial `INC-A` a `INC-D`, equipamiento `BASIC`/`SPECIALIZED`, versionado inmutable, bloqueo de publicación/activación en escenario activo, campos del registro y validaciones). *(Nota: la posibilidad de preparar borradores de reglas durante escenarios activos se mantiene abierta en `stakeholders.md` Secc. 5.2, y los formatos físicos/SQL y mecanismos técnicos de activación atómica corresponden a `PEN-005` y `PEN-006`).* | Cerrado por `DEC-016` | `DEC-010`, `DEC-016`, `business-rules.md` (`RN-EME-007` a `RN-EME-012`) |
| **PEN-002b** | *(Resuelto por `DEC-017`)* Límite máximo y política funcional de reintentos automáticos | Quedó formalizado mediante `DEC-017` (1 intento inicial + hasta 3 reintentos automáticos adicionales = máximo 4 intentos totales ante conflicto concurrente en asignación automática, reevaluación integral obligatoria, finalización anticipada, permanencia en estado `pendiente` al agotar el límite, registro funcional distinguible como `CONCURRENCY_RETRY_EXHAUSTED`, aviso visible al `Despachador` en la interfaz web y criterios `CA-CON-01` a `CA-CON-10`, sin extender el límite al modo supervisado). *(Los mecanismos técnicos de concurrencia, bloqueo, aislamiento y transacciones permanecen abiertos en `PEN-006`).* | Cerrado por `DEC-017` | `DEC-011`, `DEC-017`, `business-rules.md` (`RN-CON-005` a `RN-CON-009`, `CA-CON-01` a `CA-CON-10`) |
| **PEN-002c** | *(Resuelto por `DEC-018`)* Reglas de selección de hospital, capacidad simulada, reservas concurrentes y alta simulada | Quedó formalizado mediante `DEC-018` (capacidad numérica `C`, `R`, `O`, `D = C - R - O` con `R + O <= C`, selección tras confirmar recogida usando Dijkstra y desempate determinista, reserva confirmada previa al traslado con 1 intento inicial + hasta 2 reintentos concurrentes = máximo 3 intentos totales, espera de destino con cola FIFO sin pérdida de posición, transformación de reserva en ocupación al llegar, alta simulada explícita, independencia respecto a la liberación de ambulancia y bloqueo de edición de capacidad durante escenarios activos; reglas `RN-HOS-004` a `RN-HOS-015` y criterios `CA-HOS-01` a `CA-HOS-13`). *(Quedan diferidos en `PEN-005`, `PEN-006` y `stakeholders.md` Secc. 5: cancelación/reasignación de reservas confirmadas, permisos exactos del actor para el alta simulada y mecanismos técnicos de concurrencia y persistencia).* | Cerrado por `DEC-018` | `DEC-014`, `DEC-018`, `business-rules.md` (`RN-HOS-004` a `RN-HOS-015`, `CA-HOS-01` a `CA-HOS-13`) |
| **PEN-002d** | *(Resuelto por `DEC-019`)* Cola ante liberación de unidad básica, desempate, estados, liberación de ambulancia, grafo, tiempo simulado y métricas | Quedó formalizado mediante `DEC-019` (atención FIFO de emergencias `BASIC` al liberarse ambulancia `BASIC` en `RN-COL-007`, desempate por identificador técnico estable en `RN-ASI-005`, 7 estados de emergencia y 4 de ambulancia en `RN-RUT-007`, liberación de ambulancia al llegar al hospital e independencia frente al alta simulada en `RN-RUT-008`, grafo inicial de 10 nodos con costos abstractos y nodo aislado en `RN-RUT-006`, simulación por eventos discretos con avance manual en `RN-RUT-009`, registro experimental mínimo en `RN-MET-003` y catálogo de 7 métricas en `RN-MET-004`, junto con los criterios `CA-SIM-01` a `CA-SIM-10`). *(Se mantienen abiertos fuera de este paquete: `PEN-003` a `PEN-008`, cancelación/reasignación de reservas hospitalarias y aspectos de actores/catálogos de `stakeholders.md` Secc. 5).* | Cerrado por `DEC-019` | `DEC-007`, `DEC-012`, `DEC-013`, `DEC-018`, `DEC-019`, `business-rules.md` |
| **PEN-003** | Versiones exactas del stack tecnológico | Definir versiones concretas de Java, Spring Boot, MySQL y configuración de Maven antes de inicializar el repositorio de código. | Pendiente de aprobación | `AGENTS.md` (Secc. 2.2), `DEC-005` |
| **PEN-004** | Límites definitivos de módulos y ubicación de puertos | Validar la distribución final de módulos del monolito y la convención exacta de subpaquetes de puertos. | Pendiente de aprobación (requiere ADR) | `AGENTS.md` (Secc. 3.2, 3.3, 4.3) |
| **PEN-005** | Estrategia de persistencia, transacciones y migraciones | Definir generación de identificadores, mapeo de relaciones, estrategia de migraciones SQL, fronteras transaccionales e integridad referencial. | Pendiente de aprobación (requiere ADR) | `AGENTS.md` (Secc. 4.4) |
| **PEN-006** | Mecanismos técnicos de concurrencia en BD y aplicación | Evaluar y aprobar por operación crítica el uso de `@Version` (bloqueo optimista), bloqueo pesimista, niveles de aislamiento y ejecutores concurrentes. | Pendiente de aprobación (requiere ADR) | `AGENTS.md` (Secc. 6.3) |
| **PEN-007** | Configuración definitiva de herramientas de prueba | Confirmar configuración de JUnit 5, Mockito, Spring Boot Test, MockMvc y evaluar si se justifica Testcontainers con MySQL. | Pendiente de aprobación | `AGENTS.md` (Secc. 7.2) |
| **PEN-008** | Alineación de rutas documentales con `AGENTS.md` | `AGENTS.md` (Secciones 8.2 y 8.3) ilustra preliminarmente `docs/project/` y `docs/adr/`, mientras que la estructura autorizada en Fase 2 y Fase 3 utiliza `docs/product/` (`vision-and-scope.md`, `stakeholders.md`, `requirements/`) y `docs/decisions/` (`decision-log.md`, `adr/`). Falta autorizar la actualización de la Sección 8.2/8.3 de `AGENTS.md` para reflejar estas rutas definitivas. | Pendiente de autorización | `AGENTS.md` (Secc. 8.2, 8.3) |

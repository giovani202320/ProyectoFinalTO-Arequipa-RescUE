# AGENTS.md — Arequipa RescUE (ARE)

**Contrato de trabajo para agentes de IA y colaboradores del proyecto**

Este documento establece las instrucciones particulares para trabajar en ARE (Arequipa RescUE). Complementa las reglas globales de Gentle-AI, la metodología Organic-Driven Development (ODD) y las capacidades disponibles de Engram y Judgment Day.

Todas las decisiones deben respetar las restricciones aprobadas por el responsable del proyecto. Las propuestas técnicas no se consideran decisiones aprobadas hasta recibir autorización expresa.

---

## 1. Identidad y propósito del proyecto

### 1.1 Identificación

- **Nombre:** ARE — Arequipa RescUE.
- **Repositorio:** ProyectoFinalTO-Arequipa-RescUE.
- **Naturaleza:** proyecto académico de ingeniería de software.
- **Tipo de sistema:** simulador de atención de emergencias médicas y gestión de ambulancias.

### 1.2 Objetivo general

Desarrollar un simulador que permita representar y gestionar un proceso de atención de emergencias médicas, incluyendo el registro de solicitudes, clasificación de emergencias, asignación de ambulancias, seguimiento de unidades y gestión de recursos hospitalarios.

El sistema debe permitir estudiar y demostrar conceptos de ingeniería de software, diseño arquitectónico, algoritmos, concurrencia, persistencia de datos y desarrollo de aplicaciones web.

### 1.3 Alcance funcional preliminar

Se contempla estudiar e implementar progresivamente las siguientes capacidades:

1. **Gestión de emergencias**
   - Registro de solicitudes de atención.
   - Clasificación de emergencias mediante niveles de triage.
   - Gestión del estado de las solicitudes.
   - Control de llamadas pendientes.

2. **Gestión de ambulancias**
   - Registro y administración de ambulancias.
   - Representación de disponibilidad, ubicación y equipamiento.
   - Seguimiento de los estados operativos.

3. **Asignación y despacho**
   - Selección de ambulancias elegibles.
   - Evaluación de cercanía y equipamiento requerido.
   - Prevención de asignaciones simultáneas incompatibles.
   - Seguimiento de las asignaciones.

4. **Gestión hospitalaria**
   - Registro de hospitales.
   - Representación de disponibilidad y recursos.
   - Gestión simulada de camas y capacidad de atención.

5. **Simulación y concurrencia**
   - Simulación de solicitudes simultáneas.
   - Procesamiento concurrente de eventos.
   - Control de consistencia en las operaciones críticas.

6. **Rutas y seguimiento**
   - Representación de ubicaciones.
   - Estimación de rutas y tiempos de llegada.
   - Seguimiento del desplazamiento de ambulancias.

**Estado:** alcance preliminar. Los requisitos detallados, reglas de negocio, algoritmos y criterios de aceptación deben definirse y aprobarse antes de su implementación.

ARE es exclusivamente un simulador académico. No debe presentarse como un sistema validado para despachar ambulancias reales ni para tomar decisiones clínicas.

---

## 2. Restricciones y decisiones obligatorias

Las siguientes decisiones están aprobadas y deben respetarse:

### 2.1 Arquitectura

- El sistema utilizará una arquitectura de **monolito modular**.
- Se aplicarán principios de **arquitectura hexagonal (Ports and Adapters)**.
- La capa de presentación utilizará **MVC mediante Spring MVC y Thymeleaf**.
- Los módulos deben mantener responsabilidades delimitadas y dependencias controladas.

### 2.2 Tecnologías

El stack tecnológico acordado es:

- Lenguaje: Java.
- Framework principal: Spring Boot.
- Presentación: Spring MVC.
- Motor de vistas: Thymeleaf.
- Persistencia: Spring Data JPA y Hibernate.
- Base de datos: MySQL.
- Construcción y gestión de dependencias: Maven.

Las versiones concretas de estas tecnologías deben verificarse y aprobarse antes de establecer la configuración inicial.

### 2.3 Persistencia y dominio

Se establece una separación estricta entre:

- Modelo de dominio.
- Modelo de persistencia JPA.
- Adaptadores de persistencia.

Las clases del dominio no deben depender de Spring, JPA ni Hibernate.

Las entidades JPA deben pertenecer a infraestructura y no deben exponerse directamente a los controladores ni a las vistas.

### 2.4 Alcance y autorización

- No introducir microservicios.
- No cambiar el stack tecnológico ni la arquitectura aprobada sin autorización expresa.
- No inventar requisitos funcionales ni reglas de negocio.
- No realizar ampliaciones significativas del alcance sin aprobación.
- No implementar decisiones arquitectónicas que todavía estén pendientes de aprobación.

Las herramientas, librerías y componentes adicionales deben justificarse según su necesidad y compatibilidad. No se prohíben tecnologías futuras de manera arbitraria, pero su incorporación requiere evaluación y autorización.

---

## 3. Arquitectura del sistema

### 3.1 Estilo arquitectónico

ARE utilizará un monolito modular con principios hexagonales y MVC en presentación.

**Monolito modular**

La aplicación se desplegará como una unidad, pero estará organizada en módulos funcionales cohesivos y con bajo acoplamiento.

**Arquitectura hexagonal**

El dominio y los casos de uso deben mantenerse independientes de los detalles de infraestructura.

La interacción con sistemas externos o mecanismos técnicos se realizará mediante puertos y adaptadores.

**MVC**

La interacción web utilizará controladores Spring MVC, modelos de presentación y plantillas Thymeleaf.

MVC es el patrón de la capa de presentación y no sustituye la organización hexagonal del sistema.

### 3.2 Organización interna propuesta

Se propone que cada módulo funcional siga esta separación:

- `domain/`: entidades de dominio, objetos de valor, reglas de negocio y puertos de dominio (interfaces de repositorio y contratos propios del dominio).
- `application/`: casos de uso (puertos de entrada), servicios de aplicación, puertos de salida propios de la aplicación y coordinación de operaciones.
- `infrastructure/`: entidades JPA, implementaciones de repositorios y adaptadores técnicos que implementan los puertos definidos por el dominio y la aplicación.
- `presentation/`: controladores MVC, DTO de presentación, formularios y vistas.

Ejemplo conceptual:

```text
com.arequiparescue
├── emergency/
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── presentation/
├── ambulance/
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── presentation/
└── hospital/
    ├── domain/
    ├── application/
    ├── infrastructure/
    └── presentation/
```

Esta organización es una propuesta inicial e ilustrativa. La distribución definitiva de paquetes y módulos (incluyendo capacidades de la sección 1.3 como asignación/despacho, rutas y simulación) debe surgir del análisis de requisitos y límites de responsabilidad.

### 3.3 Regla de dependencias

Las dependencias deben apuntar hacia el interior (decisión aprobada):

- El dominio no depende de capas externas.
- La aplicación depende del dominio.
- El dominio define los puertos de persistencia/contratos de dominio y la aplicación define los puertos de casos de uso y de salida de aplicación según su responsabilidad.
- La infraestructura depende del núcleo (dominio y aplicación) para implementar dichos puertos mediante adaptadores.
- La presentación invoca casos de uso de aplicación.
- Los módulos no deben acceder directamente a la infraestructura interna de otros módulos.

La comunicación entre módulos debe realizarse mediante contratos explícitos de aplicación o mecanismos de comunicación internos debidamente justificados.

**La regla de dependencias hacia el interior es obligatoria; los límites definitivos de los módulos y la ubicación exacta de subpaquetes de puertos deben validarse y aprobarse antes de su consolidación.**

---

## 4. Modelo de dominio y persistencia

### 4.1 Independencia del dominio

Las clases de dominio deben representar conceptos y comportamientos del negocio.

No deben incluir:

- Anotaciones `@Entity`.
- Anotaciones de Hibernate.
- Dependencias de Spring.
- Dependencias de Servlet o de la capa web.

El dominio debe poder probarse sin iniciar el contexto de Spring ni conectarse a MySQL.

### 4.2 Entidades JPA

Las entidades JPA son modelos de persistencia pertenecientes a infraestructura.

Pueden contener:

- Anotaciones JPA.
- Configuración de relaciones.
- Estrategias de generación de identificadores.
- Mecanismos de versionado.
- Detalles propios del almacenamiento relacional.

No deben utilizarse directamente como modelos de entrada o salida de la presentación.

### 4.3 Adaptadores de persistencia

Los adaptadores en `infrastructure/` implementan los puertos de persistencia definidos en el núcleo (`domain/` o `application/` según la convención que se apruebe) y traducen entre el modelo de dominio y el modelo JPA.

La conversión entre ambos modelos debe ser explícita y preservar las invariantes del dominio.

La estrategia de mapeo debe evitar acoplamientos innecesarios y conversiones inconsistentes.

### 4.4 Decisiones pendientes

Se deben evaluar y documentar:

- Estrategia de identificadores.
- Mapeo de relaciones.
- Manejo de concurrencia.
- Estrategia de migraciones.
- Gestión de transacciones.
- Integridad referencial.

---

## 5. Criterios de diseño y calidad

### 5.1 Principios SOLID

Aplicar los principios SOLID de manera justificada:

- **Single Responsibility:** cada componente debe tener responsabilidades cohesionadas.
- **Open/Closed:** permitir extensiones controladas sin modificar innecesariamente comportamientos existentes.
- **Liskov Substitution:** las implementaciones deben respetar los contratos de sus abstracciones.
- **Interface Segregation:** definir interfaces pequeñas y específicas.
- **Dependency Inversion:** las políticas de alto nivel no deben depender directamente de detalles de infraestructura.

### 5.2 Separación de responsabilidades

- Los controladores MVC gestionan la interacción HTTP y delegan las operaciones.
- Los casos de uso coordinan las operaciones de aplicación.
- El dominio contiene las reglas de negocio.
- Los adaptadores de infraestructura gestionan detalles técnicos.
- Las vistas Thymeleaf presentan información y no ejecutan reglas de negocio.
- Los DTO separan los contratos de presentación de los modelos internos.

### 5.3 Patrones de diseño

Utilizar patrones únicamente cuando resuelvan problemas identificables.

Se podrán evaluar, entre otros:

- Strategy.
- Factory.
- State.
- Observer.
- Specification.
- Adapter.
- Builder.

No introducir patrones por razones exclusivamente académicas o decorativas.

Toda abstracción debe tener una justificación en términos de responsabilidades, extensibilidad, reutilización o mantenibilidad.

---

## 6. Concurrencia y consistencia de datos

### 6.1 Requisito general

La concurrencia es una característica importante de ARE.

El sistema debe preservar la integridad de las operaciones cuando existan solicitudes simultáneas, especialmente en procesos como:

- Asignación de ambulancias.
- Actualización de disponibilidad.
- Gestión de estados operativos.
- Reserva o asignación de recursos hospitalarios.

No se debe confiar exclusivamente en estructuras de datos en memoria para garantizar la consistencia de operaciones persistentes.

### 6.2 Principios de implementación

- Definir límites transaccionales coherentes con los casos de uso.
- Mantener invariantes del dominio.
- Evitar asignaciones incompatibles sobre un mismo recurso.
- Considerar las condiciones de carrera.
- Utilizar mecanismos de coordinación apropiados para cada operación.
- Mantener la base de datos como fuente de persistencia del estado del sistema.

### 6.3 Mecanismos técnicos pendientes de aprobación

Se evaluarán, según las necesidades verificadas:

- Control optimista mediante versionado JPA (`@Version`).
- Bloqueos pesimistas en operaciones críticas de asignación.
- Restricciones e índices de base de datos.
- Transacciones y niveles de aislamiento.
- Manejo de conflictos y reintentos.
- Ejecución concurrente mediante servicios y ejecutores controlados.

No aplicar simultáneamente mecanismos de bloqueo sin justificar su necesidad.

Las decisiones definitivas deben evaluarse con pruebas de concurrencia y documentarse mediante ADR.

---

## 7. Estrategia de pruebas

La calidad debe verificarse mediante pruebas proporcionales al riesgo y responsabilidad de cada componente.

### 7.1 Niveles propuestos

**Pruebas unitarias del dominio**

Validan entidades, objetos de valor, invariantes y reglas de negocio sin dependencias externas.

**Pruebas de aplicación**

Validan los casos de uso, la coordinación de operaciones y el comportamiento de los puertos.

**Pruebas de presentación**

Validan controladores, validaciones HTTP, modelos y vistas.

**Pruebas de infraestructura**

Validan adaptadores JPA, consultas, mapeos y persistencia.

**Pruebas de integración y concurrencia**

Validan operaciones completas, transacciones, consistencia y ausencia de asignaciones incompatibles.

### 7.2 Herramientas propuestas

- JUnit 5.
- Mockito.
- Spring Boot Test.
- MockMvc.
- Testcontainers, si se justifica para las pruebas de integración con MySQL.

Estas herramientas y su configuración definitiva deben verificarse y aprobarse antes de incorporarse al proyecto.

### 7.3 Criterios de calidad

- Cada funcionalidad debe tener criterios verificables.
- Las reglas críticas del dominio deben tener pruebas.
- Los cambios deben mantener las pruebas existentes.
- Los defectos deben reproducirse mediante pruebas cuando sea posible.
- Los mecanismos de concurrencia requieren pruebas específicas.
- No declarar pruebas exitosas sin evidencia de su ejecución.

---

## 8. Documentación y decisiones arquitectónicas

### 8.1 Idioma

- La documentación del proyecto se redactará en español neutro/profesional.
- Los ADR se redactarán en español neutro/profesional.
- Los textos visibles en la interfaz de usuario (plantillas Thymeleaf, etiquetas, formularios y mensajes de validación o error orientados al usuario) se redactarán en español neutro/profesional.
- Los identificadores técnicos del código se escribirán en inglés.
- Los nombres de clases, métodos, variables y paquetes deben seguir convenciones consistentes de Java.
- Los comentarios de código y Javadoc utilizarán español neutro/profesional o inglés de manera consistente, sin jerga ni modismos regionales.

### 8.2 Organización propuesta

```text
docs/
├── decisions/
│   ├── decision-log.md
│   └── adr/
├── product/
│   ├── vision-and-scope.md
│   ├── stakeholders.md
│   └── requirements/
│       └── business-rules.md
├── planning/
├── architecture/
└── testing/
```

La estructura definitiva se aprobará progresivamente.

### 8.3 Architecture Decision Records

Las decisiones arquitectónicas importantes deben registrarse mediante ADR.

Se consideran decisiones relevantes:

- Límites de módulos.
- Dependencias entre capas.
- Estrategia de persistencia.
- Gestión de transacciones.
- Concurrencia y bloqueos.
- Incorporación de tecnologías.
- Patrones arquitectónicos.
- Seguridad y despliegue.

Ubicación propuesta:

`docs/decisions/adr/ADR-XXXX-titulo-decision.md`

Estructura propuesta:

1. Estado: Propuesta, Aceptada, Rechazada o Sustituida.
2. Contexto y problema.
3. Restricciones y criterios.
4. Alternativas consideradas.
5. Decisión.
6. Consecuencias.
7. Evidencia y criterios de verificación.

Un ADR no debe marcarse como aceptado sin aprobación humana.

---

## 9. Metodología y herramientas de ingeniería

### 9.1 Gentle-AI y ODD

Se deben respetar las instrucciones globales de Gentle-AI y el flujo de Organic-Driven Development disponible en el entorno.

Este archivo complementa esas reglas, pero no pretende sustituirlas ni duplicar su implementación.

Principios particulares para ARE:

- Explorar antes de implementar.
- Identificar las decisiones pendientes.
- No asumir requisitos no documentados.
- Proponer alternativas y explicar sus consecuencias.
- Solicitar autorización para cambios relevantes.
- Implementar de manera incremental.
- Verificar los resultados.
- Mantener documentación coherente con el estado real del proyecto.

### 9.2 Gestión del trabajo

Las tareas deben tener alcance y criterios de aceptación identificables.

Los cambios complejos o tareas clasificadas como grandes bajo ODD deben dividirse en unidades de trabajo manejables y registrarse antes del primer cambio de código en `odd/tasks/<feature-name>.md` (con las secciones `## Specs`, `## Tasks` y `## Log`) junto con su espejo en Engram (`odd/<feature-name>/tasks`).

No iniciar modificaciones importantes cuando existan decisiones de arquitectura o requisitos críticos sin resolver.

### 9.3 Engram

Utilizar Engram para conservar el contexto relevante del proyecto.

Priorizar el registro de:

- Decisiones aprobadas.
- Contexto arquitectónico.
- Requisitos relevantes.
- Convenciones de implementación.
- Problemas y sus causas.
- Progreso de tareas.
- Restricciones y próximos pasos.

La memoria debe mantenerse coherente con la documentación y el estado real del repositorio.

No registrar una propuesta como una decisión aceptada.

### 9.4 Judgment Day

Utilizar los agentes `jd-judge-a` y `jd-judge-b` en paralelo y a ciegas sobre un alcance inmutable para revisiones independientes de decisiones y cambios de riesgo elevado.

Ámbitos prioritarios:

- ADR importantes.
- Límites arquitectónicos.
- Persistencia y consistencia.
- Concurrencia.
- Seguridad.
- Cambios complejos de dominio.

Los jueces deben evaluar críticamente la propuesta, identificar riesgos y justificar sus observaciones.

Tratamiento de hallazgos y ciclo operativo:

- **Hallazgos coincidentes:** los hallazgos severos confirmados por ambos jueces se corrigen mediante `jd-fix-agent` previa autorización antes de la primera ronda de corrección.
- **Hallazgos discrepantes:** cuando los jueces se contradigan, la discrepancia se escala explícitamente al responsable humano para su decisión.
- **Hallazgos críticos individuales:** cuando un hallazgo severo o crítico sea reportado por un solo juez, se registra como sospechoso (`suspect`); no se auto-corrige a ciegas ni se descarta sin análisis, sino que se verifica técnicamente contra la evidencia y se presenta al responsable humano para decidir su corrección.
- **Límite operativo:** el ciclo de revisión y corrección con `jd-fix-agent` y re-juzgamiento acotado tiene un límite máximo de **dos rondas**. Si persisten problemas tras la segunda ronda, se escala al responsable humano y se detiene el ciclo.

La decisión final corresponde al responsable humano del proyecto.

La asignación de distintos niveles de modelo no garantiza independencia entre proveedores o razonamientos. Se debe procurar una revisión verdaderamente independiente.

---

## 10. Gestión de cambios y control de versiones

- Utilizar Git para mantener el historial del proyecto.
- Realizar cambios incrementales y revisables.
- Evitar modificaciones ajenas al alcance de la tarea.
- No sobrescribir trabajo existente sin autorización.
- No realizar commits sin autorización cuando el flujo de trabajo así lo requiera.
- Mantener los mensajes de commit claros y descriptivos.
- Relacionar cambios importantes con sus tareas, pruebas y ADR correspondientes.

La estrategia de ramas, convenciones de commits y proceso de revisión se definirán durante la preparación del flujo de trabajo del repositorio.

---

## 11. Criterios de finalización

Una tarea no debe considerarse completada únicamente porque el código o documento haya sido generado.

Antes de cerrar un trabajo:

1. Verificar que el resultado responde al objetivo solicitado.
2. Revisar que respeta la arquitectura aprobada.
3. Ejecutar las pruebas pertinentes cuando corresponda.
4. Inspeccionar los cambios realizados.
5. Actualizar la documentación afectada.
6. Registrar decisiones importantes.
7. Informar claramente los resultados y las limitaciones.

Los informes de cierre deben diferenciar:

- Trabajo realizado.
- Verificaciones ejecutadas.
- Resultados observados.
- Problemas pendientes.
- Riesgos conocidos.
- Próximos pasos.

Todo cambio de código debe cerrar con la línea obligatoria de evaluación de riesgo del protocolo ODD instalado:

- `Risk: none` (cuando verifiqué que el cambio no toca ningún ítem de alto riesgo ni código compartido por comportamientos no mencionados en el pedido), o
- `Risk: item N (reason)` según las seis categorías del protocolo ODD:
  1. Datos o efectos irreversibles que no pueden reconstruirse (migraciones, alteración/borrado de datos persistidos, cambios de formato persistido).
  2. Seguridad (autenticación, permisos, credenciales, secretos, guardas, sandbox).
  3. Contratos consumidos por otros o modificaciones no solicitadas en código compartido (API pública, flags CLI, formatos de configuración, exports o helpers/parsers compartidos).
  4. Concurrencia (bloqueos, condiciones de carrera, ordenamiento asíncrono).
  5. Entrega o entorno (instaladores, release, CI, despliegue, dependencias).
  6. Ausencia de pruebas que detecten una regresión en lo modificado.

No afirmar que una verificación fue exitosa si no se ejecutó o no se dispone de evidencia.

---

## 12. Autoridad y mantenimiento de este documento

Este archivo representa las instrucciones particulares del proyecto ARE.

Las reglas globales de Gentle-AI y las decisiones aprobadas en ADR deben mantenerse coherentes con él.

Si aparece una contradicción:

1. Identificar la contradicción.
2. Determinar qué decisiones están aprobadas.
3. No resolverla mediante suposiciones.
4. Presentar las alternativas al responsable del proyecto.
5. Actualizar la documentación después de la aprobación.

Los cambios en las restricciones fundamentales, stack tecnológico o arquitectura requieren autorización expresa.

**Responsable de aprobación:** responsable humano del proyecto ARE.

**Estado del documento:** aprobado.

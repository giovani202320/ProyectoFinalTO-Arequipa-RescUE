# Modelo Arquitectónico C4 (Structurizr DSL) — ARE (Arequipa RescUE)

**Estado del documento:** Aprobado — Modelo arquitectónico C4 (Nivel 1 y Nivel 2) alineado con `DEC-001` a `DEC-021`, `ADR-0001`, `ADR-0002` y el backlog `HU-01` a `HU-14`
**Archivo DSL asociado:** [`workspace.dsl`](file:///c:/Users/giovani/Documents/GitHub/ProyectoFinalTO-Arequipa-RescUE/docs/architecture/c4/workspace.dsl)
**Propósito:** Documentar la arquitectura estática y de despliegue de alto nivel del MVP académico de **ARE – Arequipa RescUE** mediante los diagramas **C4 Nivel 1 (System Context)** y **C4 Nivel 2 (Container)** en formato **Structurizr DSL**.

---

## 1. Propósito de cada vista

| Vista Structurizr (`key`) | Nivel C4 | Propósito arquitectónico |
| :--- | :---: | :--- |
| **`C1_SystemContext`** | **Nivel 1 (System Context)** | Delimita la frontera del simulador académico **ARE — Arequipa RescUE** como una caja negra única e ilustra cómo interactúan con él los **tres actores humanos aprobados (`DEC-009`)**, evidenciando la ausencia de sistemas clínicos reales, servicios externos de mapas/GPS o integraciones de terceros (`DEC-001`, `DEC-007`, `DEC-008`). |
| **`C2_Containers`** | **Nivel 2 (Container)** | Descompone el interior del límite de **ARE — Arequipa RescUE** en sus **dos únicos contenedores de ejecución** (`Aplicación Web ARE` y `Base de Datos ARE`), situando al **Navegador Web** fuera del límite de contenedores de ARE como cliente HTTP y reflejando la decisión de **monolito modular** sin microservicios (`DEC-002`, `DEC-020` / `ADR-0001`). |

---

## 2. Elementos representados y decisiones de modelado

### 2.1 Elementos incluidos en el modelo

| Identificador DSL | Tipo C4 | Nombre visible | Ubicación | Responsabilidad |
| :--- | :---: | :--- | :--- | :--- |
| **`act01`** | `Person` | **Operador de recepción (`ACT-01`)** | Externo | Registra solicitudes de emergencia entrantes y consulta su clasificación simulada, recursos asociados y estados operativos (`HU-01`, `HU-02`, lectura en `HU-04`). |
| **`act02`** | `Person` | **Despachador (`ACT-02`)** | Externo | Supervisa el despacho (supervisado o automático), gestiona las colas FIFO (`WAITING_AMBULANCE` y `WAITING_HOSPITAL`) y confirma los estados operativos, reservas hospitalarias y entregas (`HU-02`, lectura en `HU-04`, `HU-05` a `HU-11`). |
| **`act03`** | `Person` | **Administrador (`ACT-03`)** | Externo | Administra catálogos de ambulancias, hospitales y el grafo de 10 nodos, publica versiones inmutables de reglas, ejecuta altas simuladas, controla escenarios con avance manual por eventos discretos y consulta las 7 métricas (`HU-03`, `HU-04`, `HU-12` a `HU-14`). |
| **`areSystem`** | `SoftwareSystem` | **ARE — Arequipa RescUE** | Sistema en estudio | Simulador académico de atención de emergencias médicas, despacho concurrente de ambulancias, cálculo de rutas con Dijkstra y gestión básica de capacidad hospitalaria (`DEC-001` a `DEC-021`). |
| **`webBrowser`** | `SoftwareSystem` *(tag `WebBrowser`)* | **Navegador Web** | Externo al límite de contenedores de ARE *(vista Nivel 2)* | Cliente web estándar ejecutado en el equipo del usuario que envía peticiones/formularios HTTP y muestra las vistas HTML renderizadas en el servidor por Thymeleaf. |
| **`areSystem.areWebApp`** | `Container` | **Aplicación Web ARE** | Interno a `areSystem` *(vista Nivel 2)* | Monolito modular hexagonal que expone la capa de presentación MVC y ejecuta todos los casos de uso y reglas de dominio: emergencias, ambulancias, despacho, rutas, hospitales, simulación por eventos discretos y reportes. |
| **`areSystem.areDatabase`** | `Container` | **Base de Datos ARE** | Interno a `areSystem` *(vista Nivel 2)* | Almacén relacional transaccional de catálogos, grafo (`N01`–`N10`), versiones de reglas, emergencias, ambulancias, hospitales (`C`, `R`, `O`, `D`), escenarios, eventos discretos y registros experimentales. |

### 2.2 Elementos que deliberadamente no se representan como actores ni contenedores independientes

1. **Motor de simulación (`CMP-INT-01`):** conforme a `DEC-009`, es un componente interno de software ejecutado dentro del contenedor **`Aplicación Web ARE`** (módulo `simulation`) y nunca un actor humano externo ni un proceso separado.
2. **Módulos funcionales del monolito (`emergency`, `ambulance`, `dispatch`, `routing`, `hospital`, `simulation`, `reporting`, `shared`):** conforme a `DEC-002` y `ADR-0001` (Sección 4.2), son paquetes modulares dentro del mismo artefacto desplegable **`Aplicación Web ARE`**, por lo que pertenecen a un eventual Nivel 3 (Components) y no son contenedores independientes ni microservicios.
3. **Herramientas de construcción, migración o entorno (`Maven` / `mvnw`, `Flyway`, `Docker`):** conforme a `ADR-0001`, Flyway se ejecuta embebido al iniciar la aplicación (`ddl-auto=validate`) y Maven/Docker son herramientas de construcción o infraestructura local, no contenedores lógicos de software del sistema.

---

## 3. Tecnologías aprobadas representadas (`DEC-005`, `DEC-020` / `ADR-0001`, `DEC-021` / `ADR-0002`)

| Elemento / Enlace | Tecnología especificada en el modelo | Referencia normativa |
| :--- | :--- | :--- |
| **`Aplicación Web ARE`** | `Java 21, Spring Boot 3.4.5, Spring MVC y Thymeleaf` | `DEC-005`, `DEC-020`, `ADR-0001` (Sección 4.1) |
| **`Base de Datos ARE`** | `MySQL 8.4 LTS` *(motor transaccional InnoDB)* | `DEC-005`, `DEC-020`, `ADR-0001` (Sección 4.1), `ADR-0002` |
| **Comunicación `Navegador Web` $\to$ `Aplicación Web ARE`** | `HTTP` *(formularios y vistas HTML renderizadas del lado del servidor con Spring MVC + Thymeleaf)* | `DEC-002`, `DEC-005`, `ADR-0001` |
| **Comunicación `Aplicación Web ARE` $\to$ `Base de Datos ARE`** | `JDBC/JPA` *(Spring Data JPA `3.4.x` / Hibernate `6.6.x`, transacciones cortas `READ_COMMITTED` y bloqueo pesimista `SELECT ... FOR UPDATE`)* | `DEC-003`, `DEC-020`, `DEC-021`, `ADR-0001`, `ADR-0002` |

---

## 4. Relaciones modeladas por nivel

### 4.1 Vista C4 Nivel 1 (`C1_SystemContext`)

| Origen | Destino | Descripción de la relación | Tecnología |
| :--- | :--- | :--- | :---: |
| **`Operador de recepción (ACT-01)`** | **`ARE — Arequipa RescUE`** | Registra y consulta emergencias | `HTTP` |
| **`Despachador (ACT-02)`** | **`ARE — Arequipa RescUE`** | Supervisa el despacho y los estados operativos | `HTTP` |
| **`Administrador (ACT-03)`** | **`ARE — Arequipa RescUE`** | Administra catálogos, reglas, escenarios y métricas | `HTTP` |

### 4.2 Vista C4 Nivel 2 (`C2_Containers`)

*(Nota: en `workspace.dsl` las etiquetas de contenedor y de relación se mantienen concisas para optimizar la legibilidad visual en Structurizr, reservando para este documento el detalle técnico completo).*

| Origen | Destino | Etiqueta en el diagrama (`workspace.dsl`) | Alcance técnico detallado | Tecnología |
| :--- | :--- | :--- | :--- | :---: |
| **`Operador de recepción (ACT-01)`** | **`Navegador Web`** | Registra y consulta emergencias | Opera la consola de recepción para dar de alta incidentes (`INC-A`..`INC-D`, `N01`..`N10`) y consultar estados | `Interacción web` |
| **`Despachador (ACT-02)`** | **`Navegador Web`** | Supervisa el despacho y los estados operativos | Opera la consola de despacho, las colas FIFO (`WAITING_AMBULANCE`, `WAITING_HOSPITAL`), reservas y entregas | `Interacción web` |
| **`Administrador (ACT-03)`** | **`Navegador Web`** | Administra catálogos, reglas, escenarios y métricas | Opera la consola de administración, versiones de reglas, altas simuladas, avance de eventos y métricas | `Interacción web` |
| **`Navegador Web`** | **`Aplicación Web ARE`** | Envía peticiones y recibe vistas HTML | Peticiones y formularios HTTP con vistas renderizadas del lado del servidor mediante Spring MVC y Thymeleaf | `HTTP` |
| **`Aplicación Web ARE`** | **`Base de Datos ARE`** | Lee y persiste datos transaccionalmente | Persistencia de entidades, bloqueos pesimistas de fila (`SELECT ... FOR UPDATE`), escenarios, eventos y registros experimentales | `JDBC/JPA` |

---

## 5. Cómo abrir y visualizar el modelo en Structurizr

### Opción A: Structurizr Lite mediante Docker (local)
Si tenés Docker en ejecución en tu máquina, podés levantar Structurizr Lite apuntando directamente a la carpeta `docs/architecture/c4`:

```powershell
docker run -it --rm -p 8080:8080 -v "${PWD}/docs/architecture/c4:/usr/local/structurizr" structurizr/lite
```

Luego abrí `http://localhost:8080` en el navegador y seleccioná las vistas **`C1_SystemContext`** y **`C2_Containers`**.

### Opción B: Structurizr Playground (navegador web sin instalación)
1. Abrí el entorno interactivo oficial de Structurizr DSL (`https://structurizr.com/dsl`).
2. Copiá el contenido íntegro de [`docs/architecture/c4/workspace.dsl`](file:///c:/Users/giovani/Documents/GitHub/ProyectoFinalTO-Arequipa-RescUE/docs/architecture/c4/workspace.dsl) y pegalo en el editor.
3. Alterná entre las vistas **`C1_SystemContext`** y **`C2_Containers`**.

### Opción C: Validación por línea de comandos con Structurizr CLI
Si disponés de `structurizr-cli` instalado localmente:

```powershell
structurizr validate -workspace docs/architecture/c4/workspace.dsl
```

---

## 6. Correspondencia con `ADR-0001`, `ADR-0002` y las Historias de Usuario (`HU-01` a `HU-14`)

Los dos contenedores del Nivel 2 materializan íntegramente los **7 módulos funcionales** definidos en [`ADR-0001`](file:///c:/Users/giovani/Documents/GitHub/ProyectoFinalTO-Arequipa-RescUE/docs/decisions/adr/ADR-0001-arquitectura-modular-persistencia-stack-y-pruebas.md) y las **14 historias de usuario** de [`user-stories.md`](file:///c:/Users/giovani/Documents/GitHub/ProyectoFinalTO-Arequipa-RescUE/docs/product/requirements/user-stories.md):

| Módulo interno (`ADR-0001`) | Contenedor de ejecución (`C2_Containers`) | Persistencia en `Base de Datos ARE` (`MySQL 8.4`) | Historias de usuario soportadas |
| :--- | :--- | :--- | :--- |
| **`emergency`** | `Aplicación Web ARE` (`domain`, `application`, `infrastructure`, `presentation`) | Tablas de emergencias (`REGISTERED` a `COMPLETED`) y versiones inmutables de reglas (`RUL-VXX`) | `HU-01`, `HU-02`, `HU-04` |
| **`ambulance`** | `Aplicación Web ARE` | Tabla de ambulancias (`BASIC`/`SPECIALIZED`, estados `AVAILABLE` a `TRANSPORTING`, restricción `UNIQUE (assigned_emergency_id)`) | `HU-03`, `HU-05` a `HU-08`, `HU-11` |
| **`dispatch`** | `Aplicación Web ARE` | Coordinación transaccional de elegibilidad, preservación, bloqueo pesimista (`1+3` intentos, `ADR-0002`) y cola `WAITING_AMBULANCE` | `HU-05`, `HU-06`, `HU-07`, `HU-08` |
| **`routing`** | `Aplicación Web ARE` | Tablas de nodos (`N01`–`N10`, con `N10` aislado) y aristas ponderadas para el cálculo de Dijkstra | `HU-03`, `HU-05`, `HU-09` |
| **`hospital`** | `Aplicación Web ARE` | Tablas de hospitales (`C`, `R`, `O`, `D` con `CHECK (R + O <= C)`), reservas/ocupaciones (`1+2` intentos, `ADR-0002`), cola `WAITING_HOSPITAL` y altas simuladas | `HU-03`, `HU-09`, `HU-10`, `HU-11`, `HU-12` |
| **`simulation`** | `Aplicación Web ARE` *(incluye `CMP-INT-01`)* | Tablas de escenarios experimentales, reloj lógico monotónico ($t \ge 0$) y cola de eventos discretos con desempate determinista | `HU-13` *(y soporte de reloj lógico desde `HU-01`)* |
| **`reporting`** | `Aplicación Web ARE` | Tablas de intentos/conflictos concurrentes (`CONCURRENCY_RETRY_EXHAUSTED`, `HOSPITAL_RESERVATION_RETRY_EXHAUSTED`) y consultas de las 7 métricas | `HU-07`, `HU-09`, `HU-14` |

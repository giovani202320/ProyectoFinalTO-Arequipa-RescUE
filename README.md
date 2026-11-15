# ARE — Arequipa RescUE

Simulador académico de atención de emergencias médicas y gestión de ambulancias desarrollado en Java y Spring Boot. Permite estudiar diseño arquitectónico, concurrencia, persistencia relacional, ruteo sobre grafos y simulación discreta de eventos.

> **Advertencia:** ARE es exclusivamente un simulador académico. No debe utilizarse ni presentarse como un sistema validado para despachar ambulancias reales ni para tomar decisiones clínicas.

---

## Arquitectura

ARE implementa un **monolito modular** organizado bajo principios de **arquitectura hexagonal (Ports and Adapters)**, con **Spring MVC + Thymeleaf** en la capa de presentación:

- **`domain/`**: entidades de dominio, objetos de valor, reglas de negocio y puertos de dominio sin dependencias de Spring ni JPA.
- **`application/`**: casos de uso, servicios de aplicación y coordinación transaccional.
- **`infrastructure/`**: entidades JPA, repositorios Spring Data, adaptadores de persistencia y migraciones Flyway.
- **`presentation/`**: controladores Spring MVC, modelos de vista y plantillas Thymeleaf.

---

## Stack tecnológico

Versiones verificadas en [`pom.xml`](pom.xml), [`compose.yaml`](compose.yaml) y recursos locales:

| Componente | Tecnología / Versión |
|---|---|
| Lenguaje | Java 21 (LTS) |
| Framework principal | Spring Boot 3.4.5 |
| Presentación | Spring MVC + Thymeleaf |
| Persistencia | Spring Data JPA / Hibernate (`ddl-auto=validate`) |
| Migraciones | Flyway (`flyway-core`, `flyway-mysql`) |
| Base de datos | MySQL 8.4 LTS (InnoDB, UTF-8 / UTC) |
| Frontend local | Bootstrap 5.3.3 y Lucide 0.469.0 (`src/main/resources/static/vendor/`) |
| Construcción | Maven Wrapper (`mvnw` / `mvnw.cmd`, Apache Maven 3.9.9) |
| Pruebas | JUnit 5, Mockito, AssertJ, Spring Boot Test y MockMvc |

---

## Módulos implementados

Bajo `src/main/java/com/arequiparescue/`:

- **`emergency`**: registro de emergencias, clasificación de triaje determinista y administración de reglas de triaje.
- **`ambulance`**: catálogo y estados operativos de ambulancias (`BASIC` / `SPECIALIZED`).
- **`hospital`**: catálogo hospitalario, capacidad (`C / R / O / D`), reservas, entregas y altas simuladas.
- **`dispatch`**: evaluación de elegibilidad, despacho supervisado, despacho automático con reintentos y colas FIFO (`WAITING_AMBULANCE` y `WAITING_HOSPITAL`).
- **`routing`**: representación del grafo urbano (`N01`–`N10`) y cálculo de rutas mínimas (Dijkstra).
- **`simulation`**: gestión de escenarios experimentales y avance manual del reloj lógico por eventos discretos.
- **`reporting`**: métricas operativas agregadas y consulta de trazabilidad.
- **`shared`**: utilidades transversales, layout común Thymeleaf y formateadores de presentación (`AreUiFormatter`).

---

## Requisitos de instalación

- **JDK 21** instalado y disponible en `PATH` (`java -version`).
- **Docker** con **Docker Compose** (recomendado para levantar MySQL 8.4) o una instancia local de **MySQL 8.4 LTS**.
- **Git** para clonar el repositorio.
- No requiere instalación previa de Maven gracias al **Maven Wrapper** incluido.

---

## Configuración inicial desde un clon limpio

### 1. Clonar el repositorio

```powershell
git clone https://github.com/giovani202320/ProyectoFinalTO-Arequipa-RescUE.git
cd ProyectoFinalTO-Arequipa-RescUE
```

### 2. Variables de entorno

ARE no almacena contraseñas ni secretos en el repositorio. Consulte [`.env.example`](.env.example) para ver todas las variables admitidas por [`compose.yaml`](compose.yaml) y [`src/main/resources/application.properties`](src/main/resources/application.properties):

| Variable | Uso | Valor por defecto |
|---|---|---|
| `ARE_DB_NAME` | Nombre del esquema principal en Docker Compose | `arequipa_rescue` |
| `ARE_DB_USERNAME` | Usuario de conexión MySQL | `are_user` |
| `ARE_DB_PASSWORD` | Contraseña de `ARE_DB_USERNAME` | *Obligatoria (sin valor por defecto)* |
| `ARE_DB_ROOT_PASSWORD` | Contraseña `root` para inicializar MySQL en Docker | *Obligatoria en Docker Compose* |
| `ARE_DB_PORT` | Puerto expuesto en el host | `3306` |
| `ARE_DB_URL` | URL JDBC utilizada por Spring Boot | `jdbc:mysql://localhost:3306/arequipa_rescue?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `ARE_SERVER_PORT` | Puerto HTTP de Spring Boot | `8080` |

En Windows PowerShell (sesión actual):

```powershell
$env:ARE_DB_PASSWORD = "su_password_seguro"
$env:ARE_DB_ROOT_PASSWORD = "su_root_password_seguro"
```

También puede copiar `.env.example` a `.env` (ignorado por Git) para uso con `docker compose`.

### 3. Opción A: MySQL 8.4 con Docker Compose (recomendada)

```powershell
docker compose up -d
```

Esto crea el contenedor `are-mysql` con la base inicial `arequipa_rescue` y el usuario `are_user`.

### 4. Opción B: MySQL instalado localmente

Si utiliza una instalación local de MySQL 8.4 LTS:

```sql
CREATE DATABASE IF NOT EXISTS arequipa_rescue CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS 'are_user'@'localhost' IDENTIFIED BY 'su_password_seguro';
GRANT ALL PRIVILEGES ON arequipa_rescue.* TO 'are_user'@'localhost';
FLUSH PRIVILEGES;
```

### 5. Migraciones Flyway (`V1`–`V7`)

Al iniciar la aplicación, Flyway ejecuta automáticamente las migraciones ubicadas en `src/main/resources/db/migration/`:

- `V1__init_schema.sql`
- `V2__seed_initial_graph.sql`
- `V3__create_emergencies_and_triage_rules.sql`
- `V4__create_catalogs_and_scenarios_tables.sql`
- `V5__create_dispatch_traceability_tables.sql`
- `V6__create_hospital_reservations_table.sql`
- `V7__create_simulation_events_table.sql`

---

## Compilación y ejecución

En Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

En Linux / macOS:

```bash
./mvnw spring-boot:run
```

Una vez iniciada la aplicación, acceda desde el navegador a:

- `http://localhost:8080/`

---

## Pruebas automatizadas

### Pruebas unitarias y MVC (sin requerir MySQL activo)

```powershell
.\mvnw.cmd clean test
```

Las pruebas de integración con MySQL quedan omitidas automáticamente si no se activa el indicador `-Dare.mysql.integration=true`.

### Pruebas de integración MySQL (`arequipa_rescue_test`)

Las pruebas de integración utilizan una base aislada definida en [`src/test/resources/application-mysql-test.properties`](src/test/resources/application-mysql-test.properties) (`arequipa_rescue_test`), para no alterar los datos manuales de `arequipa_rescue`.

1. Crear la base de pruebas en MySQL y otorgar permisos a `are_user`:

```powershell
docker exec -i are-mysql mysql -u root -p"$env:ARE_DB_ROOT_PASSWORD" -e "CREATE DATABASE IF NOT EXISTS arequipa_rescue_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; GRANT ALL PRIVILEGES ON arequipa_rescue_test.* TO 'are_user'@'%'; FLUSH PRIVILEGES;"
```

2. Ejecutar la suite completa con integración MySQL:

```powershell
.\mvnw.cmd test "-Dare.mysql.integration=true"
```

---

## Estructura general del proyecto

```text
.
├── compose.yaml
├── pom.xml
├── AGENTS.md
├── docs/
│   ├── architecture/
│   │   ├── c4/                  # Modelo C4 (workspace.dsl y README.md)
│   │   └── uml/                 # Diagramas de casos de uso y máquinas de estado
│   ├── decisions/
│   │   ├── decision-log.md      # Registro de decisiones aprobadas
│   │   └── adr/                 # ADR-0001 y ADR-0002
│   └── product/
│       ├── vision-and-scope.md
│       ├── stakeholders.md
│       └── requirements/        # user-stories.md y business-rules.md
└── src/
    ├── main/
    │   ├── java/com/arequiparescue/
    │   └── resources/
    │       ├── application.properties
    │       ├── db/migration/    # Migraciones Flyway V1–V7
    │       ├── static/          # CSS/JS propios y vendor (Bootstrap, Lucide)
    │       └── templates/       # Vistas Thymeleaf
    └── test/
        ├── java/com/arequiparescue/
        └── resources/
            └── application-mysql-test.properties
```

---

## Documentación normativa y de arquitectura

- Contrato del proyecto: [`AGENTS.md`](AGENTS.md)
- Registro de decisiones: [`docs/decisions/decision-log.md`](docs/decisions/decision-log.md)
- Decisiones de arquitectura: [`ADR-0001`](docs/decisions/adr/ADR-0001-arquitectura-modular-persistencia-stack-y-pruebas.md) y [`ADR-0002`](docs/decisions/adr/ADR-0002-concurrencia-bloqueo-pesimista-transacciones-y-reintentos.md)
- Modelo C4 y diagramas UML: [`docs/architecture/c4/README.md`](docs/architecture/c4/README.md) y [`docs/architecture/uml/`](docs/architecture/uml/)
- Requisitos e historias de usuario: [`docs/product/requirements/user-stories.md`](docs/product/requirements/user-stories.md) y [`docs/product/requirements/business-rules.md`](docs/product/requirements/business-rules.md)

---

## Limitaciones conocidas

- Proyecto académico de simulación; no apto para operación clínica ni despacho real.
- El grafo urbano (`N01`–`N10`) y las reglas de simulación responden al alcance controlado del MVP documentado en `docs/product/`.
- La ejecución de pruebas de integración MySQL requiere la base `arequipa_rescue_test` previamente creada y la variable `ARE_DB_PASSWORD` configurada en el entorno.
- No incluye autenticación de usuarios ni despliegue productivo containerizado de la aplicación Java (el contenedor provisto en `compose.yaml` corresponde únicamente a MySQL).

---

## Colaboración

1. Cree una rama descriptiva desde `main` (por ejemplo: `feat/mi-cambio` o `fix/mi-correccion`).
2. Mantenga los cambios acotados y acompañados de sus pruebas correspondientes.
3. Utilice mensajes de commit claros bajo el formato Conventional Commits (`feat:`, `fix:`, `docs:`, `test:`, etc.).
4. Verifique que `.\mvnw.cmd clean test` (y las pruebas de integración cuando aplique) pasen antes de abrir un Pull Request hacia `main`.

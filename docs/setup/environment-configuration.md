# Configuración del entorno de ARE

Referencia exacta de las variables de entorno leídas por `compose.yaml`, `src/main/resources/application.properties` y `src/test/resources/application-mysql-test.properties`.

---

## 1. Variables reconocidas

| Variable | Archivo que la consume | Valor por defecto | Descripción |
|---|---|---|---|
| `ARE_SERVER_PORT` | `application.properties` | `8080` | Puerto HTTP de Spring Boot (`server.port`). |
| `ARE_DB_URL` | `application.properties` | `jdbc:mysql://localhost:3306/arequipa_rescue?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` | URL JDBC de la aplicación. Cámbiela si apunta a `arequipa_rescue_demo` o a otro puerto. |
| `ARE_DB_USERNAME` | `application.properties`, `compose.yaml` | `are_user` | Usuario MySQL para la aplicación, pruebas y contenedor Docker. |
| `ARE_DB_PASSWORD` | `application.properties`, `compose.yaml` | *Vacío en `application.properties`; obligatorio en `compose.yaml`* | Contraseña de `ARE_DB_USERNAME`. Debe definirse en el entorno antes de iniciar. |
| `ARE_DB_NAME` | `compose.yaml` | `arequipa_rescue` | Nombre de la base inicial creada al levantar el contenedor `are-mysql` por primera vez. |
| `ARE_DB_ROOT_PASSWORD` | `compose.yaml` | *Obligatoria en `compose.yaml`* | Contraseña del usuario `root` en el contenedor Docker MySQL. |
| `ARE_DB_PORT` | `compose.yaml`, `application-mysql-test.properties` | `3306` | Puerto MySQL expuesto en el host y usado en el fallback de `application-mysql-test.properties`. |
| `ARE_DB_TEST_URL` | `application-mysql-test.properties` | `jdbc:mysql://localhost:${ARE_DB_PORT:3306}/arequipa_rescue_test?...` | URL JDBC exclusiva del perfil `mysql-test` (`arequipa_rescue_test`). |

---

## 2. Cómo se cargan las variables

1. **Docker Compose (`docker compose up -d`):** lee automáticamente el archivo `.env` en la raíz del proyecto (si existe) junto con las variables del sistema.
2. **Spring Boot y Maven Wrapper (`.\mvnw.cmd`):** **no leen el archivo `.env` automáticamente**. Las variables deben existir en la sesión de PowerShell (por ejemplo, `$env:ARE_DB_PASSWORD = "..."`) o como variables de entorno del sistema operativo.
3. **Pruebas de integración MySQL (`-Dare.mysql.integration=true`):** activan el perfil `mysql-test` y usan `arequipa_rescue_test` mediante `ARE_DB_TEST_URL` / `ARE_DB_PORT`, sin fallback hacia `arequipa_rescue`.

---

## 3. Política de seguridad y Git

- El archivo `.env` está listado en `.gitignore` y **nunca debe subirse al repositorio**.
- Versione únicamente `.env.example` con valores de ejemplo (`CHANGE_ME_*`).
- `spring.jpa.hibernate.ddl-auto=validate` y `spring.flyway.enabled=true` permanecen activos tanto en ejecución normal como en `mysql-test`.

# Instalación de ARE en Windows (PowerShell)

Guía paso a paso para instalar y ejecutar **Arequipa RescUE (ARE)** en una computadora Windows desde un clon limpio del repositorio.

---

## 1. Requisitos previos

- **Git** instalado y disponible en PowerShell (`git --version`).
- **JDK 21 (LTS)** instalado y configurado en `PATH` (`java -version`).
- **Motor MySQL 8.4 LTS**, mediante una de estas dos opciones:
  - **Opción A (recomendada):** Docker Desktop con Docker Compose (`docker compose version`).
  - **Opción B:** Instalación local de MySQL 8.4 LTS.
- **No se requiere Maven instalado globalmente:** el proyecto incluye el **Maven Wrapper** (`.\mvnw.cmd`).

Verifique Java antes de continuar:

```powershell
java -version
```

---

## 2. Clonar el repositorio

```powershell
git clone https://github.com/giovani202320/ProyectoFinalTO-Arequipa-RescUE.git
cd ProyectoFinalTO-Arequipa-RescUE
```

---

## 3. Configuración de MySQL y bases de datos

### Opción A: Instalación con Docker Compose (recomendada)

1. Copie `.env.example` a `.env` (`.env` está ignorado por Git y no debe subirse al repositorio):

```powershell
Copy-Item .env.example .env
```

2. Edite `.env` y reemplace `CHANGE_ME_APP_PASSWORD` y `CHANGE_ME_ROOT_PASSWORD` por contraseñas locales seguras.
3. Levante el contenedor `are-mysql`:

```powershell
docker compose up -d
```

4. Cree la base aislada de pruebas `arequipa_rescue_test` (y opcionalmente `arequipa_rescue_demo`) otorgando permisos a `are_user`:

```powershell
"CREATE DATABASE IF NOT EXISTS arequipa_rescue_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS arequipa_rescue_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
GRANT ALL PRIVILEGES ON arequipa_rescue_test.* TO 'are_user'@'%';
GRANT ALL PRIVILEGES ON arequipa_rescue_demo.* TO 'are_user'@'%';
FLUSH PRIVILEGES;" | docker exec -i are-mysql sh -c 'mysql -u root -p"$MYSQL_ROOT_PASSWORD"'
```

### Opción B: Instalación con MySQL 8.4 local

Ejecute en el cliente `mysql` como `root` (reemplazando `SU_PASSWORD_SEGURO` por su contraseña local):

```sql
CREATE DATABASE IF NOT EXISTS arequipa_rescue CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS arequipa_rescue_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS arequipa_rescue_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'are_user'@'localhost' IDENTIFIED BY 'SU_PASSWORD_SEGURO';
GRANT ALL PRIVILEGES ON arequipa_rescue.* TO 'are_user'@'localhost';
GRANT ALL PRIVILEGES ON arequipa_rescue_test.* TO 'are_user'@'localhost';
GRANT ALL PRIVILEGES ON arequipa_rescue_demo.* TO 'are_user'@'localhost';
FLUSH PRIVILEGES;
```

---

## 4. Variables de entorno en PowerShell

Copiar `.env` permite que **Docker Compose** lea las variables, pero **Spring Boot (`.\mvnw.cmd`) no carga `.env` automáticamente**. Antes de iniciar la aplicación o las pruebas MySQL en PowerShell, exporte las variables en la sesión actual:

```powershell
$env:ARE_DB_USERNAME = "are_user"
$env:ARE_DB_PASSWORD = "SU_PASSWORD_SEGURO"
$env:ARE_DB_URL = "jdbc:mysql://localhost:3306/arequipa_rescue?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:ARE_SERVER_PORT = "8080"
```

---

## 5. Pruebas automatizadas

Suite unitaria y MVC (sin requerir bandera MySQL):

```powershell
.\mvnw.cmd clean test
```

Suite completa incluyendo pruebas de integración contra `arequipa_rescue_test`:

```powershell
.\mvnw.cmd test "-Dare.mysql.integration=true"
```

---

## 6. Iniciar la aplicación y acceder en el navegador

Al iniciar Spring Boot, Flyway ejecuta automáticamente las migraciones `V1`–`V7` sobre la base configurada en `ARE_DB_URL`:

```powershell
.\mvnw.cmd spring-boot:run
```

Una vez iniciado el servidor, abra en el navegador:

- `http://localhost:8080`

---

## 7. Diagnóstico de errores frecuentes

| Síntoma | Causa probable | Solución |
|---|---|---|
| `Access denied for user 'are_user'` | `ARE_DB_PASSWORD` no está definida en PowerShell o no coincide con MySQL | Exporte `$env:ARE_DB_PASSWORD` con la misma contraseña usada al crear el usuario o definida en `.env`. |
| `Unknown database 'arequipa_rescue_test'` | No se creó la base aislada de pruebas | Cree `arequipa_rescue_test` y otorgue permisos a `are_user` (paso 3). |
| `Port 8080 was already in use` | Otro proceso ocupa el puerto `8080` | Defina `$env:ARE_SERVER_PORT = "8081"` antes de ejecutar `.\mvnw.cmd spring-boot:run`. |
| `Communications link failure` en puerto distinto de `3306` | MySQL corre en otro puerto | Ajuste `$env:ARE_DB_PORT`, `$env:ARE_DB_URL` y `$env:ARE_DB_TEST_URL`. |
| `Release version 21 not supported` | `JAVA_HOME` o `PATH` apunta a un JDK anterior a 21 | Instale JDK 21 y verifique `java -version`. |

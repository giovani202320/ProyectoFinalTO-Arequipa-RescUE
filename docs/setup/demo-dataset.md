# Dataset de demostración (`arequipa_rescue_demo`)

El script [`demo-seed.sql`](demo-seed.sql) permite poblar una base aislada **`arequipa_rescue_demo`** con un conjunto mínimo y coherente de recursos y emergencias para demostraciones reproducibles, sin alterar `arequipa_rescue` ni `arequipa_rescue_test`.

---

## 1. Qué incluye el dataset

- **Topología y reglas:** provistas por Flyway (`V2` carga nodos `N01`–`N10` y 12 aristas; `V3` carga la versión activa `RUL-V01`).
- **Ambulancias (`4` unidades en estado `AVAILABLE`):**
  - `AMB-01` (`SPECIALIZED`, nodo `N01`)
  - `AMB-02` (`BASIC`, nodo `N02`)
  - `AMB-03` (`BASIC`, nodo `N04`)
  - `AMB-04` (`SPECIALIZED`, nodo `N06`)
- **Hospitales (`3` establecimientos habilitados, `R = 0`, `O = 0`):**
  - `HOSP-01` — `Hospital General Demo` (nodo `N05`, `C = 5`)
  - `HOSP-02` — `Hospital Norte Demo` (nodo `N07`, `C = 3`)
  - `HOSP-03` — `Posta Capacidad Mínima` (nodo `N03`, `C = 1`, útil para probar cola hospitalaria `WAITING_HOSPITAL` FIFO)
- **Emergencias (`3` registradas en estado `REGISTERED`, `t = 0`):**
  - `EMG-00001` (`INC-A`, `N01`, requiere `SPECIALIZED`)
  - `EMG-00002` (`INC-B`, `N03`, requiere `BASIC`)
  - `EMG-00003` (`INC-C`, `N10`, caso opcional de nodo aislado sin conexión activa)
- **Secuencias actualizadas:** `AMBULANCE = 5`, `HOSPITAL = 4`, `EMERGENCY = 4` para evitar colisiones al crear nuevos registros desde la UI.

> Los estados avanzados (`ASSIGNED`, `AT_INCIDENT`, `WAITING_HOSPITAL`, `IN_TRANSPORT`, `COMPLETED`) se alcanzan ejecutando el flujo real de la aplicación durante la demostración.

---

## 2. Garantías de seguridad del script

- Aborta con `SIGNAL SQLSTATE '45000'` si `DATABASE()` no es exactamente `arequipa_rescue_demo`.
- Aborta si Flyway `V1`–`V7` no ha finalizado (`version = '7'` y `success = 1`).
- Aborta de forma segura sin modificar nada si `ambulances`, `hospitals` o `emergencies` ya contienen registros.
- Se ejecuta dentro de `START TRANSACTION ... COMMIT`, sin desactivar `FOREIGN_KEY_CHECKS` ni usar `TRUNCATE`, `DROP` o `DELETE`.

---

## 3. Procedimiento de uso (PowerShell + Docker)

1. Crear la base aislada `arequipa_rescue_demo` y otorgar permisos a `are_user` (si no existe):

```powershell
"CREATE DATABASE IF NOT EXISTS arequipa_rescue_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
GRANT ALL PRIVILEGES ON arequipa_rescue_demo.* TO 'are_user'@'%';
FLUSH PRIVILEGES;" | docker exec -i are-mysql sh -c 'mysql -u root -p"$MYSQL_ROOT_PASSWORD"'
```

2. Arrancar Spring Boot una vez apuntando a `arequipa_rescue_demo` para que Flyway aplique `V1`–`V7` (y luego detenerlo con `Ctrl+C` si desea cargar el seed antes de la demo, o cargarlo con el servidor detenido):

```powershell
$env:ARE_DB_URL = "jdbc:mysql://localhost:3306/arequipa_rescue_demo?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
.\mvnw.cmd spring-boot:run
```

3. Cargar [`demo-seed.sql`](demo-seed.sql) sobre `arequipa_rescue_demo`:

```powershell
Get-Content docs/setup/demo-seed.sql -Raw | docker exec -i are-mysql sh -c 'mysql -u root -p"$MYSQL_ROOT_PASSWORD" arequipa_rescue_demo'
```

4. Iniciar ARE nuevamente sobre `arequipa_rescue_demo` y abrir `http://localhost:8080`.

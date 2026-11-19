# ADR-0003: Persistencia histórica de segmentos de ruta Dijkstra por emergencia (`emergency_route_segments`)

- **Estado:** Aprobada (autorización condicionada 2026-11-18)
- **Fecha:** 2026-11-18
- **Proyecto:** ARE — Arequipa RescUE (`ProyectoFinalTO-Arequipa-RescUE`)
- **Decisiones vinculadas:** `ADR-0001`, `ADR-0002` (`DEC-020`, `DEC-021`), `DEC-013`, `DEC-018`, `DEC-019`

---

## 1. Contexto y problema

Durante la validación funcional de la Fase 5A (`A07-4`), se comprobó que al finalizar la atención de una emergencia (`IN_TRANSPORT` o `COMPLETED`), la ficha solo puede reconstruir el **Tramo B** (`Emergencia → Hospital reservado`) utilizando el nodo del incidente y la ubicación actual del hospital reservado, pero **no puede reconstruir el Tramo A** (`Ambulancia → Emergencia`), porque la ubicación de la ambulancia cambia al llegar al incidente (`AT_INCIDENT`) y `emergencies` no persiste el nodo de partida ni el camino/costo calculado por `DijkstraRouteCalculator` en el instante de la asignación.

---

## 2. Restricciones y criterios

- No modificar `DijkstraRouteCalculator`, políticas de elegibilidad (`AmbulanceEligibilityPolicy`, `HospitalEligibilityPolicy`), reglas de triaje, concurrencia (`ADR-0002`), reloj lógico ni fórmulas `MET-01` a `MET-07`.
- No editar migraciones Flyway ya publicadas (`V1`–`V7`).
- Preservar intactas las emergencias históricas existentes sin inventar ni estimar rutas retroactivas.
- Garantizar que los reintentos concurrentes o conflictos no generen segmentos falsos ni duplicados (`unique` por `(emergency_id, segment_type)`).
- Mantener la separación hexagonal (`domain` libre de JPA/Spring; adaptador en `infrastructure/`).

---

## 3. Alternativas consideradas

### 3.1 Estrategia de almacenamiento
- **Alternativa A (Propuesta): Tabla independiente `emergency_route_segments` (Flyway `V8`) con clave única `(emergency_id, segment_type)`.** Separa la trazabilidad histórica de rutas del agregado operativo `emergencies`, preserva intacto el esquema de `V3` y permite modelar explícitamente `AMBULANCE_TO_INCIDENT` (Tramo A) e `INCIDENT_TO_HOSPITAL` (Tramo B) con sus invariantes y claves foráneas.
- **Alternativa B (Descartada): Agregar columnas `dispatch_*` y `hospital_route_*` directamente en `emergencies`.** Mezcla historial de rutas de múltiples tramos en la tabla principal de estado y escala peor si se agregan metadatos por segmento.

### 3.2 Representación de `path_nodes`
- **Alternativa A (Propuesta): Columna `path_nodes VARCHAR(512) NOT NULL` en formato canónico separado por comas (`N01,N02,N05`) o JSON array.** En el formato `N01,N02,N05` (o JSON validado en Java/MySQL) resulta compacto, legible en SQL y determinista al mapear `List<NodeId>` en `infrastructure/` sin requerir librerías adicionales.

---

## 4. Decisión propuesta

### 4.1 Esquema relacional (`V8__create_emergency_route_segments_table.sql`)
Crear la tabla `emergency_route_segments` en MySQL 8.4 (InnoDB, `utf8mb4`):

- `id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY`
- `emergency_id VARCHAR(32) NOT NULL` (`FK -> emergencies(id)`)
- `segment_type VARCHAR(32) NOT NULL` (`CHECK (segment_type IN ('AMBULANCE_TO_INCIDENT', 'INCIDENT_TO_HOSPITAL'))`)
- `resource_id VARCHAR(32) NOT NULL` (`AMB-XX` o `HOSP-XX`)
- `origin_node_id VARCHAR(32) NOT NULL` (`FK -> graph_nodes(id)`)
- `destination_node_id VARCHAR(32) NOT NULL` (`FK -> graph_nodes(id)`)
- `path_nodes VARCHAR(512) NOT NULL` (lista ordenada no vacía de `NodeId` donde `first == origin_node_id` y `last == destination_node_id`)
- `abstract_cost INT NOT NULL` (`CHECK (abstract_cost >= 0)`)
- `calculated_at_t BIGINT NOT NULL` (`CHECK (calculated_at_t >= 0)`)
- `CONSTRAINT uq_emergency_route_segment UNIQUE (emergency_id, segment_type)`

### 4.2 Puntos transaccionales de captura (conforme a `ADR-0002`)
1. **Tramo A (`AMBULANCE_TO_INCIDENT`):** dentro de `SupervisedDispatchAttemptExecutor.executeAssignmentAttemptInternal` (`Propagation.REQUIRES_NEW`, bajo bloqueo `SELECT ... FOR UPDATE` de `Emergency -> Ambulance`), inmediatamente **antes** de actualizar `ambulance.assignToEmergency(...)`, persistir el segmento utilizando la ruta y el costo del `EligibleAmbulanceCandidate` validado en ese mismo intento. Cubre despacho supervisado, automático y desde cola FIFO `WAITING_AMBULANCE`. Si el intento falla o hace rollback, no queda ningún segmento persistido.
2. **Tramo B (`INCIDENT_TO_HOSPITAL`):** dentro de `HospitalReservationAttemptExecutor` (`Propagation.REQUIRES_NEW`, bajo bloqueo `SELECT ... FOR UPDATE` de `Emergency -> Ambulance -> Hospital`), en la misma transacción en que se confirma `HospitalReservation` y `emergency.reserveHospitalAndStartTransport(...)`. Cubre reserva normal, reintentos concurrentes y cola FIFO `WAITING_HOSPITAL`.

---

## 5. Consecuencias

- **Positivas:** toda nueva emergencia que complete el ciclo conserva ambos tramos reales, inmutables y auditables tras `COMPLETED` y tras reinicios; la ficha muestra Tramo A (azul), Tramo B (violeta), solapamiento y costo combinado real.
- **Compatibilidad:** las emergencias históricas previas a `V8` permanecen intactas sin segmentos inventados; la UI informa claramente que el dato histórico no está disponible.
- **Migración:** antes de aplicar `V8` en `arequipa_rescue`, se realizará respaldo verificable (`mysqldump`) y autorización explícita.

---

## 6. Evidencia y criterios de verificación

- Pruebas unitarias de invariantes del modelo de segmento (costo `>= 0`, secuencia no vacía, primer/último nodo).
- Pruebas de integración MySQL (`arequipa_rescue_test`) cubriendo los 12 escenarios: asignación supervisada, automática, cola FIFO, conflicto concurrente sin segmento falso, reserva hospitalaria, reserva fallida, reintentos sin duplicados, entrega, `COMPLETED`, emergencias históricas, `N10` aislado y reinicio.

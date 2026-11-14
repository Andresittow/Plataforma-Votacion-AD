# CONTRACTS.md — Contratos compartidos

Cualquier cambio aquí debe avisarse a la otra persona.

## 1. Kafka

Broker interno: `kafka:29092` (desde Docker) / `localhost:9092` (desde el host). 3 particiones, replication factor 1.

| Topic | Cleanup | Key | Value | Productor | Consumidores |
|---|---|---|---|---|---|
| `eligible_voters` | `compact` | `userId` (String) | `VoterRecord` JSON | voter-registration-service | voting-api |
| `raw_votes` | `compact` | `userId` (String) | `VoteEvent` JSON | voting-api | vote-processor, regional-rollup-service, bot-detector, analytics-archiver |
| `security_alerts` | `delete` | `ip` | alerta JSON (Rol C) | bot-detector | — |

Serialización: String key + JSON (Jackson) como String value. Un valor `null` (tombstone) en `eligible_voters` significa "votante eliminado".

### Consumer groups (uno distinto por servicio)
`voting-api-eligibility` (sin commit, lee siempre desde el inicio), `vote-processor-group`, `regional-rollup-group`, `bot-detector-group`, `analytics-archiver-group`.

### JSON de mensajes (formato "plano": los VOs se serializan como string)

`VoterRecord`:
```json
{ "userId": "u-001", "region": "norte", "active": true }
```
`VoteEvent`:
```json
{ "userId": "u-001", "candidateId": "c-02", "region": "norte", "ip": "190.12.4.7", "timestamp": "2026-11-14T15:30:00Z" }
```
Regiones válidas: `norte`, `sur`, `este`, `oeste`, `centro` (minúsculas). La región del voto la toma **voting-api del registro del votante** (no del cliente), para que no se pueda falsear.

## 2. RabbitMQ

Host interno `rabbitmq:5672` (usuario/clave `guest`/`guest` en dev). Mensajes JSON, `content_type: application/json`.

| Exchange | Tipo | Routing key | Publica | Consume |
|---|---|---|---|---|
| `live_results_global` | fanout | (ignorada) | vote-processor cada **1 s** | global-dashboard-worker |
| `live_results_regional` | topic | `results.<region>` (ej. `results.norte`) | regional-rollup-service cada **5 s**, un mensaje por región | regional-dashboard-worker (binding `results.*`) |

Los consumidores usan una **cola exclusiva y anónima** (server-named, auto-delete) enlazada al exchange.

Mensaje global:
```json
{ "totals": { "c-01": 120, "c-02": 98 }, "totalVotes": 218, "at": "2026-11-14T15:30:01Z" }
```
Mensaje regional:
```json
{ "region": "norte", "totals": { "c-01": 40, "c-02": 31 }, "weightedTotals": { "c-01": 44.0, "c-02": 34.1 }, "strategy": "PopulationWeighting", "at": "2026-11-14T15:30:05Z" }
```

## 3. Semántica "un voto por usuario"
`raw_votes` está compactado con key=`userId`, pero la compactación es eventual. **Los consumidores con estado no deben confiar en ella**: mantienen un `Map<VoterId, VoteEvent>` y, si llega un voto de un usuario que ya votó, **reemplazan** el anterior (restan el candidato viejo, suman el nuevo). Cuenta siempre el último voto.

## 4. Módulo `common-domain` (lo crea Persona A; paquete `com.votingplatform.common`)

Dependencias: solo `jackson-annotations`, `jackson-databind`, `jackson-datatype-jsr310`. Sin Spring.

`domain/`:
- `record VoterId(String value)` — no nulo, no vacío, máx. 64 chars, sin espacios (trim aplicado). `@JsonValue` en `value()`, `@JsonCreator` en el constructor.
- `record CandidateId(String value)` — mismas reglas.
- `record Region(String value)` — normaliza a minúsculas; debe ser una de `norte,sur,este,oeste,centro`. Mismo mapeo JSON plano.
- `record IpAddress(String value)` — valida formato IPv4 o IPv6 válido. Mismo mapeo JSON plano.
- `record VoterRecord(VoterId userId, Region region, boolean active)`.
- `record VoteEvent(VoterId userId, CandidateId candidateId, Region region, IpAddress ip, Instant timestamp)` — todos no nulos. Método `static VoteEvent of(...)` opcional.

`port/` (interfaces para DIP):
- `interface VotePublisher { void publish(VoteEvent vote); }`
- `interface EligibleVoterStore { boolean isEligible(VoterId id); Optional<VoterRecord> find(VoterId id); void apply(VoterRecord record); void remove(VoterId id); }`

Cada servicio define sus propios puertos adicionales dentro de su módulo (ej. `ResultsPublisher` en vote-processor).

Nombres de campos JSON = nombres de los componentes del record (`userId`, `candidateId`, ...).

## 5. Puertos HTTP y variables de entorno

| Servicio | Puerto | Variables |
|---|---|---|
| voting-api | 8080 | `KAFKA_BOOTSTRAP_SERVERS` |
| global-dashboard-worker | 8081 (WebSocket `/ws/global`, página `/dashboard.html`) | `RABBITMQ_HOST` |
| regional-dashboard-worker | 8082 (WebSocket `/ws/regional`, página `/dashboard.html`) | `RABBITMQ_HOST` |
| vote-processor | 8083 (solo actuator) | `KAFKA_BOOTSTRAP_SERVERS`, `RABBITMQ_HOST` |
| regional-rollup-service | 8084 (solo actuator) | `KAFKA_BOOTSTRAP_SERVERS`, `RABBITMQ_HOST` |
| voter-registration-service | 8085 (solo actuator) | `KAFKA_BOOTSTRAP_SERVERS`, `VOTERS_FILE` (default `classpath:voters.json`) |

Valores por defecto en `application.yml` para correr desde el host: `localhost:9092` y `localhost`.

## 6. Ejemplo de `voters.json`
```json
[
  { "userId": "u-001", "region": "norte" },
  { "userId": "u-002", "region": "sur" }
]
```
(El archivo real debe tener ~50 votantes repartidos en las 5 regiones, y 2-3 entradas inválidas a propósito para probar el rechazo fail-fast.)

## 7. API de voting-api
`POST /vote` body `{ "userId": "u-001", "candidateId": "c-02" }`
- 200 → `{ "status": "ACCEPTED" }`
- 400 → body inválido (Bean Validation)
- 403 → votante no elegible (inexistente o `active=false`)
- 503 → la tabla de elegibles aún no terminó de cargarse

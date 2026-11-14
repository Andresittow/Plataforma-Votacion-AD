# Prompts — Persona B (Procesamiento y distribución) · Codex · rama `feature/procesamiento`

Antes de todo: clona el repo, `git checkout develop && git pull` y luego `git checkout -b feature/procesamiento`. Envía cada prompt **por separado y en orden**.

Tus módulos: `vote-processor`, `regional-rollup-service`, `global-dashboard-worker`, `regional-dashboard-worker`. No edites `common-domain` ni el pom padre (son de la Persona A; si necesitas un cambio, pídeselo).

Nota de arranque: el esqueleto Maven (A0) y `common-domain` (A1) los sube la Persona A a `develop` durante la primera hora. B1 y B2 están pensados para que empieces YA con dominio puro; cuando A1 esté en develop haz `git pull --rebase origin develop` y conecta tus clases a `VoteEvent`, `VoterId`, `CandidateId` y `Region` reales.

---

## Prompt B1 — `vote-processor` (agregado global + Fanout)

```
Lee AGENTS.md y CONTRACTS.md (secciones 1, 2, 3 y 4) y síguelos estrictamente. Trabaja SOLO en vote-processor.

Primero dominio + tests (TDD), luego infraestructura.

Dominio (paquete domain, Java puro):
- Agregado rico `GlobalTally` SIN setters. Estado interno privado: Map<VoterId, CandidateId> con el último voto de cada usuario y Map<CandidateId, Long> con los totales.
- Método `void registerVote(VoteEvent vote)`: si el usuario ya votó, resta 1 al candidato anterior y suma 1 al nuevo (un voto por usuario, cuenta el último; un reintento al mismo candidato no cambia nada). No acepta nulos.
- `TallySnapshot snapshot(Instant at)` devuelve un record inmutable (copia defensiva) con `Map<String,Long> totals`, `long totalVotes`, `Instant at`, con la forma del JSON "Mensaje global" del contrato.
- Evita candidatos con total 0 en el snapshot.
- Hilo-seguro: los consumidores Kafka y el scheduler acceden a la vez (usa synchronized o lock interno; sin exponer colecciones mutables).

Aplicación:
- Puerto `ResultsPublisher { void publishGlobal(TallySnapshot snapshot); }`.
- `VoteProcessingService(GlobalTally tally, ResultsPublisher publisher, Clock clock)` por constructor, con `onVote(VoteEvent)` y `publishSnapshot()`.

Tests (JUnit 5 + Mockito, AAA, sin Spring): conteo básico; un usuario que cambia de voto mueve su voto; reintento idéntico no duplica; snapshot inmutable (modificar el mapa devuelto no afecta al agregado); publishSnapshot() llama exactamente 1 vez al publisher con los totales correctos (usa ArgumentCaptor y Clock fijo); acceso concurrente con varios hilos da un total consistente.

Infraestructura:
- `RawVotesKafkaListener` (@KafkaListener topic raw_votes, groupId `vote-processor-group`, auto-offset-reset=earliest, deserializa JSON a VoteEvent con el ObjectMapper configurado con JavaTimeModule) que delega en VoteProcessingService. Ignora y loguea mensajes malformados sin romper el consumidor.
- `RabbitResultsPublisher implements ResultsPublisher` con RabbitTemplate y Jackson2JsonMessageConverter: publica a exchange fanout `live_results_global`. Declara el exchange como bean (FanoutExchange, durable).
- `@Scheduled(fixedRate = 1000)` que llama a publishSnapshot() (activar @EnableScheduling en config).

Criterio: `mvn -pl vote-processor -am test` en verde. Prueba manual con docker-compose.dev.yml levantado: produce votos de prueba y verifica en la UI de RabbitMQ (localhost:15672) que el exchange live_results_global recibe un mensaje por segundo.
```

---

## Prompt B2 — `regional-rollup-service` (Strategy + Topic Exchange)

```
Lee AGENTS.md y CONTRACTS.md. Trabaja SOLO en regional-rollup-service. Primero dominio + tests, luego infraestructura.

Dominio (Java puro):
- Interfaz `WeightingStrategy { String name(); double weightFor(Region region); }`. Implementaciones: `UniformWeighting` (peso 1.0 para todas) y `PopulationWeighting` (pesos configurables por región recibidos por constructor, p. ej. norte 1.2, sur 0.9, este 1.0, oeste 1.1, centro 1.0; fail-fast si falta una región o el peso es <= 0). Sin if/else ni switch por región fuera de la estrategia.
- Agregado rico `RegionalTally` SIN setters, con la misma semántica de un voto por usuario que `GlobalTally` (guarda el último voto de cada usuario; si cambia de candidato, mueve su voto; si cambia de región, también). Método `registerVote(VoteEvent)` y `List<RegionalSnapshot> snapshots(WeightingStrategy strategy, Instant at)` que devuelve un snapshot inmutable por región con `totals` (conteo crudo) y `weightedTotals` (conteo × peso), `strategy` = strategy.name(), según el JSON "Mensaje regional" del contrato.

Aplicación:
- Puerto `RegionalResultsPublisher { void publishRegional(RegionalSnapshot snapshot); }`.
- `RegionalRollupService(RegionalTally tally, WeightingStrategy strategy, RegionalResultsPublisher publisher, Clock clock)` por constructor: `onVote(VoteEvent)` y `publishSnapshots()` (un publish por región que tenga votos).

Tests (JUnit 5 + Mockito, AAA, sin Spring): cada estrategia (pesos correctos, fail-fast de configuración); el servicio funciona IGUAL intercambiando la estrategia (parametrized test con ambas); agrupación correcta por región; re-voto y cambio de región; publishSnapshots llama al publisher una vez por región con ArgumentCaptor; no publica regiones sin votos.

Infraestructura:
- `RawVotesKafkaListener` con groupId `regional-rollup-group` (earliest).
- `RabbitRegionalResultsPublisher`: TopicExchange `live_results_regional`, routing key `results.<region>`.
- Selección de estrategia por propiedad `rollup.strategy` (uniform | population) mediante una @Configuration que crea el bean WeightingStrategy.
- `@Scheduled(fixedRate = 5000)`.

Criterio: tests en verde y verificación manual en la UI de RabbitMQ de que se publican mensajes con routing keys results.norte, results.sur, etc.
```

---

## Prompt B3 — `global-dashboard-worker` y `regional-dashboard-worker`

```
Lee AGENTS.md y CONTRACTS.md (secciones 2 y 5). Trabaja SOLO en global-dashboard-worker y regional-dashboard-worker.

Cada worker: consume de RabbitMQ y reenvía por WebSocket nativo (spring-boot-starter-websocket, TextWebSocketHandler, sin STOMP) a todos los navegadores conectados, y sirve su dashboard.html desde src/main/resources/static.

Diseño (dominio/aplicación primero, DIP):
- Puerto `ResultsBroadcaster { void broadcast(String jsonPayload); }`.
- `ResultsRelayService(ResultsBroadcaster broadcaster)`: recibe el JSON de RabbitMQ, valida que sea JSON no vacío (descarta y loguea los inválidos) y lo reenvía. Para el regional, añade además una caché con el último resultado por región para enviar el estado actual a un cliente que se acaba de conectar.
- `WebSocketResultsBroadcaster implements ResultsBroadcaster`: mantiene un conjunto thread-safe de WebSocketSession, elimina las cerradas y envía a las activas. Handler en /ws/global o /ws/regional según el contrato.

Tests (JUnit 5 + Mockito, AAA, sin Spring): el relay reenvía un payload válido exactamente 1 vez; descarta payload vacío o no-JSON; el regional guarda el último por región y lo entrega a un cliente nuevo; el broadcaster elimina sesiones cerradas y no falla si una sesión lanza IOException (mock de WebSocketSession).

Infraestructura RabbitMQ:
- global: declara FanoutExchange `live_results_global`, una cola anónima exclusiva auto-delete (AnonymousQueue) y su Binding; @RabbitListener sobre esa cola.
- regional: declara TopicExchange `live_results_regional`, AnonymousQueue y Binding con routing key `results.*`; @RabbitListener.
- Recibe el mensaje como String (no deserializar a objeto).

dashboard.html (un solo archivo, sin frameworks ni CDN, JS vanilla):
- global: barras por candidato con total y porcentaje, total de votos, indicador de conexión y reconexión automática del WebSocket.
- regional: tarjetas/barras por región (norte, sur, este, oeste, centro) mostrando conteo crudo y ponderado, y la estrategia usada.

Criterio: tests en verde; con vote-processor / regional-rollup-service corriendo y votos de prueba, el dashboard se actualiza en vivo en http://localhost:8081/dashboard.html y :8082.
```

---

## Prompt B4 — Dockerfiles, script de pruebas y E2E de tu parte

```
Lee la sección "Docker" de AGENTS.md.

1. Crea un Dockerfile multi-stage en vote-processor/, regional-rollup-service/, global-dashboard-worker/ y regional-dashboard-worker/ (contexto = raíz del repo; builder maven:3.9.6-eclipse-temurin-17-alpine copiando primero los pom.xml para cachear dependencias; runtime eclipse-temurin:17-jre-alpine, usuario appuser, flags -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError). Si la Persona A ya creó .dockerignore en la raíz, úsalo; si no, créalo.
2. Crea scripts/send_test_votes.sh: genera N votos (parámetro, 200 por defecto) con userId u-001..u-050, candidatos c-01..c-03, regiones y IPs aleatorias válidas, y los publica en raw_votes con key=userId usando `docker compose -f docker-compose.dev.yml exec -T kafka kafka-console-producer --property parse.key=true --property key.separator='|'`. Incluye algunos usuarios repetidos con distinto candidato para probar "último voto".
3. Verifica que los 4 Dockerfiles construyen y que `docker run --rm <imagen> id` no devuelve root.
4. Documenta en docs/PROCESAMIENTO.md cómo arrancar y probar tus 4 servicios y el flujo de prueba con el script.
```

---

## Prueba de integración conjunta (cuando ambos terminen)

1. `docker compose -f docker-compose.dev.yml up -d`
2. A: arranca `voter-registration-service` y luego `voting-api`.
3. B: arranca `vote-processor`, `regional-rollup-service` y los dos dashboards.
4. Hacer 10-20 `curl POST /vote` con usuarios elegibles, uno repetido con otro candidato y uno inexistente (403).
5. Ver el cambio en `http://localhost:8081/dashboard.html` y `:8082/dashboard.html`.

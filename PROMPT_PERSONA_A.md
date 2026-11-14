# Prompts — Persona A (Ingesta) · Antigravity · rama `feature/ingesta`

Antes de todo: clona el repo, `git checkout develop && git pull` y luego `git checkout -b feature/ingesta`. Envía cada prompt **por separado y en orden**; no pases al siguiente hasta que compile y los tests estén en verde.

Tus módulos: parent `pom.xml`, `common-domain`, `voter-registration-service`, `voting-api`.

---

## Prompt A0 — Esqueleto Maven (URGENTE: hacer push a `develop` en ~20 min, la otra persona depende de esto)

```
Lee AGENTS.md y CONTRACTS.md en la raíz del repo y síguelos estrictamente.

Tarea: crea el esqueleto Maven multi-módulo. No escribas lógica todavía.

1. pom.xml padre (packaging pom, Java 17, Spring Boot 3.2.x vía spring-boot-dependencies en dependencyManagement; NO usar spring-boot-starter-parent como parent para poder controlar el aggregator). Módulos:
   common-domain, voter-registration-service, voting-api, vote-processor, regional-rollup-service, global-dashboard-worker, regional-dashboard-worker.
   Configura maven-compiler-plugin (release 17) y maven-surefire-plugin en pluginManagement. Dependencias de test comunes en dependencyManagement: junit-jupiter, mockito-junit-jupiter, assertj-core.
2. common-domain/pom.xml: jar, solo jackson-annotations, jackson-databind, jackson-datatype-jsr310 + dependencias de test.
3. Para cada uno de los otros 6 módulos: carpeta + pom.xml mínimo (parent, dependencia a common-domain, spring-boot-maven-plugin con repackage, y dependencias según el módulo):
   - voter-registration-service y voting-api: spring-boot-starter (voting-api además starter-web y starter-validation), spring-kafka, starter-actuator.
   - vote-processor y regional-rollup-service: spring-boot-starter, spring-kafka, spring-boot-starter-amqp, actuator.
   - global-dashboard-worker y regional-dashboard-worker: starter-web, starter-websocket, starter-amqp.
   - Todos: spring-boot-starter-test con exclusión de testcontainers (no usarlo), mockito, assertj.
   Cada módulo con su clase @SpringBootApplication vacía en com.votingplatform.<modulo> y un application.yml con los valores por defecto del contrato (puerto, KAFKA_BOOTSTRAP_SERVERS=localhost:9092, RABBITMQ_HOST=localhost).
4. .gitignore para Java/Maven/IDEs.

Criterio de aceptación: `mvn -q -DskipTests package` pasa en la raíz. Haz commit y push de este esqueleto a la rama develop.
```

---

## Prompt A1 — `common-domain` (value objects + puertos)

```
Lee AGENTS.md y CONTRACTS.md (sección 4). Trabaja SOLO en common-domain.

TDD: por cada clase, escribe primero el test y luego la implementación.

Implementa en com.votingplatform.common.domain: VoterId, CandidateId, Region, IpAddress (records con validación fail-fast en el constructor compacto, mapeo JSON plano con @JsonValue/@JsonCreator), VoterRecord y VoteEvent (records con requireNonNull). En com.votingplatform.common.port: VotePublisher y EligibleVoterStore. Exactamente como dice el contrato.

Tests JUnit 5 + AssertJ (patrón AAA, sin Spring) que verifiquen:
- rechazo de nulos, vacíos, espacios, regiones fuera del catálogo e IPs inválidas;
- normalización (trim, minúsculas en Region);
- igualdad por valor entre records;
- round-trip JSON con ObjectMapper (+JavaTimeModule): serializar un VoteEvent produce EXACTAMENTE el JSON plano del contrato y se puede deserializar de vuelta (timestamp como ISO-8601 string, WRITE_DATES_AS_TIMESTAMPS desactivado).

Criterio: `mvn -pl common-domain test` en verde. Haz commit y push a develop (la otra persona hará pull de esto).
```

---

## Prompt A2 — `voter-registration-service`

```
Lee AGENTS.md y CONTRACTS.md. Trabaja SOLO en voter-registration-service (depende de common-domain).

Primero dominio + tests, luego infraestructura.

Dominio / aplicación:
- `VoterRegistrationService(VoterSource source, EligibleVoterPublisher publisher)` con inyección por constructor. Método `RegistrationReport registerAll()`: lee los registros crudos del source, intenta convertir cada uno en VoterRecord (los VOs validan fail-fast), publica los válidos y cuenta/loguea los inválidos SIN abortar el proceso. `RegistrationReport` es un record (publicados, rechazados).
- Puertos propios: `VoterSource { List<RawVoter> readAll(); }` y `EligibleVoterPublisher { void publish(VoterRecord record); }`. `RawVoter` es un record con strings crudos (userId, region).
- Evita duplicados: si un userId aparece dos veces en el archivo, se publica solo una vez (última gana) y se reporta.

Tests con Mockito (sin Spring, patrón AAA): publica solo válidos, ignora inválidos y los cuenta, deduplica, no publica nada con fuente vacía, verifica con `verify` el número exacto de interacciones con el publisher.

Infraestructura:
- `JsonFileVoterSource` (lee `VOTERS_FILE`, por defecto classpath:voters.json con Jackson).
- `KafkaEligibleVoterPublisher` con KafkaTemplate<String,String>: topic `eligible_voters`, key=userId, value=JSON del VoterRecord.
- `ApplicationRunner` que ejecuta registerAll() al arrancar y loguea el reporte. Configuración con constructor injection.
- src/main/resources/voters.json con ~50 votantes en las 5 regiones + 3 entradas inválidas a propósito.

Criterio: tests en verde; con `docker compose -f docker-compose.dev.yml up -d` corriendo, arrancar el servicio y verificar con `kafka-console-consumer --from-beginning` que los mensajes llegan a eligible_voters con la key correcta.
```

---

## Prompt A3 — `voting-api`

```
Lee AGENTS.md y CONTRACTS.md (secciones 1, 4 y 7). Trabaja SOLO en voting-api.

Primero dominio/aplicación + tests, luego infraestructura.

Aplicación:
- `VoteService(EligibleVoterStore store, VotePublisher publisher, Clock clock)` por constructor. Método `VoteResult submit(VoterId userId, CandidateId candidateId, IpAddress ip)`:
  * busca al votante en el store; si no existe o no está activo → lanza `VoterNotEligibleException` (excepción de dominio) y NO publica;
  * si es elegible → construye el VoteEvent con la región del registro del votante y el timestamp del Clock, publica y devuelve `VoteResult.ACCEPTED`.
- `ReadinessGate` (o similar) que indica si la tabla de elegibles terminó de cargarse; VoteService lanza `EligibilityNotReadyException` si no.
- `InMemoryEligibleVoterStore implements EligibleVoterStore` con ConcurrentHashMap: apply() inserta/actualiza, remove() elimina; el estado solo cambia con esos métodos.

Tests (JUnit 5 + Mockito, sin Spring, AAA): elegible → publica exactamente 1 vez con los datos correctos; no elegible → excepción y `verifyNoInteractions(publisher)`; votante inactivo → excepción; no listo → excepción; el timestamp sale del Clock fijo; el store aplica tombstones (remove).

Infraestructura:
- `EligibleVotersKafkaLoader`: en un hilo propio usa un KafkaConsumer (o KafkaConsumer vía ConsumerFactory) SIN group commit: `assign` de todas las particiones de eligible_voters + `seekToBeginning`, y hace poll continuo. Por cada registro: value no nulo → store.apply(VoterRecord), value nulo (tombstone) → store.remove. Marca el ReadinessGate cuando alcanza los end offsets que había al arrancar; sigue consumiendo después para mantener el estado sincronizado.
- `KafkaVotePublisher implements VotePublisher` con KafkaTemplate<String,String>: topic `raw_votes`, key=userId, value=JSON del VoteEvent. Configura acks=all y enable.idempotence.
- `VoteController` POST /vote con `VoteRequestRecord(@NotBlank String userId, @NotBlank String candidateId)` + @Valid. La IP sale de la request (respeta X-Forwarded-For si existe). Mapea respuestas con @RestControllerAdvice: 200 {"status":"ACCEPTED"}, 400 validación, 403 no elegible, 503 no listo.
- Test del controller con MockMvc standalone (sin contexto Spring completo, service mockeado) para verificar los códigos 200/400/403/503.

Criterio: tests en verde. Prueba manual con curl: votante del voters.json → 200; userId inexistente → 403; body vacío → 400; el mensaje aparece en raw_votes con la key correcta.
```

---

## Prompt A4 — Dockerfiles + prueba E2E de tu parte

```
Lee la sección "Docker" de AGENTS.md.

Crea un Dockerfile multi-stage en voter-registration-service/ y en voting-api/ (contexto de build = raíz del repo, builder con maven:3.9.6-eclipse-temurin-17-alpine copiando primero los pom.xml del padre y de los módulos para cachear dependencias, runtime eclipse-temurin:17-jre-alpine con usuario appuser y las flags -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError). Añade un .dockerignore en la raíz (target/, .git/, .idea/).

Verifica: `docker build -f voting-api/Dockerfile -t voting-api .` y lo mismo para voter-registration-service; confirma con `docker run --rm voting-api id` que no corre como root. Documenta en un archivo docs/INGESTA.md cómo arrancar y probar tus dos servicios.
```

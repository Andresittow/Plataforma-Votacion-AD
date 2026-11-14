# AGENTS.md — Reglas del proyecto

Proyecto: plataforma de votación en vivo (Kafka + RabbitMQ + PostgreSQL + Spring Boot). Lee **CONTRACTS.md** antes de escribir código: es la fuente de verdad para topics, JSON, exchanges y clases comunes. No inventes nombres distintos a los del contrato.

## Stack
- Java 17, Spring Boot 3.2.x, Maven multi-módulo, spring-kafka, spring-boot-starter-amqp, Jackson.
- Paquete raíz: `com.votingplatform.<modulo>` (ej. `com.votingplatform.votingapi`).
- Tests: JUnit 5 + Mockito + AssertJ.

## Reglas de diseño (obligatorias, se evalúan)
1. **Dominio rico, no anémico**: sin setters en entidades/agregados. El estado cambia solo mediante métodos con nombre de negocio (`registerVote`, `markBlocked`...) que protegen invariantes y lanzan excepciones de dominio.
2. **Value Objects con `record`** (VoterId, CandidateId, VoteEvent...) con validación fail-fast en el constructor compacto (`IllegalArgumentException` / `NullPointerException` vía `Objects.requireNonNull`).
3. **DIP**: la lógica de dominio/servicio depende de **interfaces (puertos)**, nunca de KafkaTemplate, RabbitTemplate ni repositorios JPA directamente. Los adaptadores (Kafka/Rabbit) implementan los puertos.
4. **Inyección solo por constructor**. Prohibido `@Autowired` en campos y prohibido `@Data`/`@Setter` de Lombok (no usar Lombok).
5. **Strategy** y **State** donde lo pida el prompt: sin cadenas de `if/else` ni `switch` sobre tipos/estados.
6. Separa capas dentro de cada módulo: `domain` (Java puro, sin imports de Spring/Kafka/Rabbit), `application` (servicios), `infrastructure` (adaptadores, config, controladores).

## Tests unitarios
- Patrón **AAA** (Arrange / Act / Assert) con comentarios `// Arrange`, `// Act`, `// Assert`.
- **Prohibido** `@SpringBootTest`, Testcontainers, Kafka/Rabbit/Postgres reales o embebidos. Usa Mockito (`@ExtendWith(MockitoExtension.class)`) para los puertos.
- Deben correr en milisegundos. Nombres descriptivos: `shouldRejectVote_whenVoterIsNotEligible`.
- Orden de trabajo: primero dominio + tests, después el adaptador de infraestructura.

## Docker (cada microservicio)
- Dockerfile **multi-stage** en la carpeta del módulo, contexto de build = raíz del repo (`docker build -f <modulo>/Dockerfile .`).
- Etapa 1 `builder`: `maven:3.9.6-eclipse-temurin-17-alpine`; copiar primero los `pom.xml` (padre + módulos) y resolver dependencias para aprovechar caché; después copiar fuentes y `mvn -pl <modulo> -am package -DskipTests`.
- Etapa 2 `runtime`: `eclipse-temurin:17-jre-alpine`, usuario no root `appuser`, solo el `.jar`.
- `ENTRYPOINT` con `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError`.
- Configuración por variables de entorno (`KAFKA_BOOTSTRAP_SERVERS`, `RABBITMQ_HOST`...), nunca hosts hardcodeados.

## Flujo de trabajo
- Trabaja solo en los módulos que te asignen. No edites módulos ajenos ni `common-domain`.
- Antes de dar por terminada una tarea: `mvn -pl <modulo> -am test` debe pasar en verde.
- Commits pequeños y descriptivos (`feat(voting-api): ...`, `test(vote-processor): ...`).

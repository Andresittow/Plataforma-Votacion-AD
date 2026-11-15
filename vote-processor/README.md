# vote-processor

Procesa raw_votes con el ultimo voto recibido por usuario y publica snapshots
en live_results_global cada segundo. El estado es local a una instancia:
ejecutar una sola replica para obtener el conteo global.

## Dependencia compartida

Requiere los records de com.votingplatform.common.domain definidos en CONTRACTS.md:
VoterId, CandidateId, Region, IpAddress y VoteEvent. No se duplican en este modulo.

## Pruebas unitarias

Desde la raiz: `mvn -pl vote-processor -am test`.
JUnit 5, Mockito y AssertJ; sin brokers ni contexto Spring.

## Prueba manual

1. Desde la raiz: `docker compose -f docker-compose.dev.yml up -d`.
   Esperar a que kafka-init finalice correctamente.
2. Ejecutar `mvn -pl vote-processor -am package -DskipTests` y
   `java -jar vote-processor/target/vote-processor-1.0.0-SNAPSHOT.jar`.
3. Abrir http://localhost:15672 con guest/guest. En Exchanges abrir
   live_results_global y comprobar el ritmo de publicacion (~1 mensaje/s).
   Para inspeccionar cuerpos, crear una cola temporal y enlazarla al exchange
   con routing key vacia antes de enviar los votos.
4. Abrir el productor desde otra terminal:
   `docker compose -f docker-compose.dev.yml exec kafka kafka-console-producer --bootstrap-server kafka:29092 --topic raw_votes --property parse.key=true --property key.separator=|`
5. Pegar estas lineas, una por mensaje:

```text
u-001|{"userId":"u-001","candidateId":"c-01","region":"norte","ip":"190.12.4.7","timestamp":"2026-11-14T15:30:00Z"}
u-002|{"userId":"u-002","candidateId":"c-01","region":"sur","ip":"190.12.4.8","timestamp":"2026-11-14T15:30:01Z"}
u-001|{"userId":"u-001","candidateId":"c-02","region":"norte","ip":"190.12.4.7","timestamp":"2026-11-14T15:30:02Z"}
u-001|{"userId":"u-001","candidateId":"c-02","region":"norte","ip":"190.12.4.7","timestamp":"2026-11-14T15:30:02Z"}
```

6. En un entorno sin otros votos, comprobar totals={"c-01":1,"c-02":1},
   totalVotes=2 y at en formato ISO-8601. Un mensaje malformado debe generar
   un warning y permitir procesar el siguiente voto valido.

Docker: `docker build -f vote-processor/Dockerfile .`.
Para ejecutar desde Docker configurar KAFKA_BOOTSTRAP_SERVERS y RABBITMQ_HOST
con los hosts de la red correspondiente.

## Estado de validacion en esta entrega

Maven fue ejecutado, pero la compilacion esta bloqueada porque common-domain
todavia no contiene las clases del contrato. No se modifico ese modulo.
La prueba manual no se ejecuto: el daemon Docker Desktop no estaba disponible.
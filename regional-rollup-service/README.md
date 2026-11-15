# regional-rollup-service

Mantiene el ultimo voto recibido por usuario, incluyendo cambios de candidato
y region. Publica un mensaje por region con votos cada cinco segundos en
live_results_regional con routing key results.<region>. Los snapshots no
incluyen candidatos ni regiones sin votos.

Estado en memoria: ejecutar una sola replica para obtener todos los resultados.
El grupo Kafka conserva offsets; reiniciar con offsets ya confirmados no
reconstruye el historial completo. Para una reconstruccion de desarrollo,
detener el servicio y restablecer los offsets del grupo al inicio.

## Configuracion

rollup.strategy acepta uniform (predeterminado) o population.
Usar ROLLUP_STRATEGY=population y configurar ROLLUP_WEIGHTS_NORTE,
ROLLUP_WEIGHTS_SUR, ROLLUP_WEIGHTS_ESTE, ROLLUP_WEIGHTS_OESTE y
ROLLUP_WEIGHTS_CENTRO. Valores predeterminados: 1.2, 0.9, 1.0, 1.1 y 1.0.
PopulationWeighting exige las cinco regiones y pesos finitos mayores que cero.

KAFKA_BOOTSTRAP_SERVERS y RABBITMQ_HOST seleccionan los brokers.
RABBITMQ_PORT, RABBITMQ_USERNAME y RABBITMQ_PASSWORD son configurables.

## Pruebas

Desde la raiz: `mvn -pl regional-rollup-service -am test`.
No requieren Spring, Docker ni brokers.

## Verificacion manual

1. Ejecutar `docker compose -f docker-compose.dev.yml up -d` desde la raiz
   y esperar a que kafka-init termine.
2. Ejecutar `mvn -pl regional-rollup-service -am package -DskipTests`.
3. Arrancar el servicio con pesos:
   `java -jar regional-rollup-service/target/regional-rollup-service-1.0.0-SNAPSHOT.jar --rollup.strategy=population`.
4. Abrir http://localhost:15672 (guest/guest). En Exchanges comprobar que
   live_results_regional es topic y durable. Crear una cola temporal y
   enlazarla al exchange con results.* para inspeccionar los mensajes.
5. Abrir un productor:
   `docker compose -f docker-compose.dev.yml exec kafka kafka-console-producer --bootstrap-server kafka:29092 --topic raw_votes --property parse.key=true --property key.separator=|`
6. Pegar estos mensajes (en un entorno sin otros votos):

```text
u-reg-001|{"userId":"u-reg-001","candidateId":"c-01","region":"norte","ip":"190.12.4.7","timestamp":"2026-11-14T15:30:00Z"}
u-reg-002|{"userId":"u-reg-002","candidateId":"c-02","region":"sur","ip":"190.12.4.8","timestamp":"2026-11-14T15:30:01Z"}
```

7. En la cola, usar Get messages: cada cinco segundos aparecen results.norte
   (c-01: 1, weightedTotals c-01: 1.2) y results.sur
   (c-02: 1, weightedTotals c-02: 0.9), strategy PopulationWeighting y at ISO-8601.
8. Enviar el cambio y su reintento:

```text
u-reg-001|{"userId":"u-reg-001","candidateId":"c-02","region":"sur","ip":"190.12.4.7","timestamp":"2026-11-14T15:30:02Z"}
u-reg-001|{"userId":"u-reg-001","candidateId":"c-02","region":"sur","ip":"190.12.4.7","timestamp":"2026-11-14T15:30:02Z"}
```

9. Los siguientes ciclos publican solo results.sur con c-02: 2 y weightedTotals
   c-02: 1.8. No se publican nuevos mensajes de norte.
10. Reiniciar con --rollup.strategy=uniform y votos nuevos para comprobar
    que weightedTotals coincide con totals y strategy es UniformWeighting.

Docker: `docker build -f regional-rollup-service/Dockerfile .`.
La verificacion manual queda pendiente cuando Docker Desktop no esta activo.
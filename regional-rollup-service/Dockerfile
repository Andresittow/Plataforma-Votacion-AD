FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /workspace
COPY pom.xml .
COPY common-domain/pom.xml common-domain/pom.xml
COPY voter-registration-service/pom.xml voter-registration-service/pom.xml
COPY voting-api/pom.xml voting-api/pom.xml
COPY vote-processor/pom.xml vote-processor/pom.xml
COPY regional-rollup-service/pom.xml regional-rollup-service/pom.xml
COPY global-dashboard-worker/pom.xml global-dashboard-worker/pom.xml
COPY regional-dashboard-worker/pom.xml regional-dashboard-worker/pom.xml
RUN mvn -B -pl regional-rollup-service -am dependency:go-offline
COPY common-domain/src common-domain/src
COPY regional-rollup-service/src regional-rollup-service/src
RUN mvn -B -pl regional-rollup-service -am package -DskipTests

FROM eclipse-temurin:17-jre-alpine AS runtime
RUN addgroup -S appuser && adduser -S appuser -G appuser
WORKDIR /app
COPY --from=builder /workspace/regional-rollup-service/target/regional-rollup-service-*.jar app.jar
USER appuser
EXPOSE 8084
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
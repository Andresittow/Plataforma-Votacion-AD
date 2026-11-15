FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /workspace

COPY pom.xml .
COPY common-domain/pom.xml common-domain/pom.xml
COPY voter-registration-service/pom.xml voter-registration-service/pom.xml
RUN mvn -pl voter-registration-service -am dependency:go-offline -DskipTests

COPY common-domain/src common-domain/src
COPY voter-registration-service/src voter-registration-service/src
RUN mvn -pl voter-registration-service -am package -DskipTests

FROM eclipse-temurin:17-jre-alpine AS runtime
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app
COPY --from=builder /workspace/voter-registration-service/target/voter-registration-service-*.jar app.jar
USER appuser

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]

# syntax=docker/dockerfile:1
# Zabora subscription-service — imagen de producción (multi-stage)

FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app

COPY pom.xml .
RUN mvn -q -B dependency:go-offline -DskipTests || true

COPY src ./src
RUN mvn -q -B clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring -u 1000

COPY --from=builder /app/target/subscription-service-*.jar app.jar
RUN chown spring:spring app.jar

USER spring

# Spring Boot: server.port vía variable estándar
ENV SERVER_PORT=8004
EXPOSE 8004

# Opcional: añade curl/wget en la imagen si quieres HEALTHCHECK contra /actuator/health
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

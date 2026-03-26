# =====================
# Stage 1: Build
# =====================
FROM maven:3.9.3-eclipse-temurin-17 AS builder

WORKDIR /app

# Copiar pom.xml primero para cachear dependencias
COPY pom.xml .

# Descargar dependencias offline (aprovecha cache de Docker)
RUN mvn dependency:go-offline -B

# Copiar código fuente
COPY src ./src

# Construir el JAR sin ejecutar tests
RUN mvn clean package -DskipTests

# =====================
# Stage 2: Run
# =====================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Crear usuario no-root para seguridad
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Copiar el JAR generado desde la etapa de build
COPY --from=builder /app/target/subscription-service-1.0.0.jar app.jar

# Exponer el puerto que usa el microservicio
ENV PORT=8004
EXPOSE ${PORT}

# Ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
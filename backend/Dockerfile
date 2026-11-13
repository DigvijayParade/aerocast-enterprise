# Multi-stage Docker build for Spring Boot 3
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Cache dependencies layer
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Lightweight JRE runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV PORT=8000
EXPOSE 8000
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT}"]

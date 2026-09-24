# Dockerfile multi-stage para construir y ejecutar la aplicación Spring Boot
# Basado en Java 21 y Maven

# Etapa de construcción
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml mvnw* .
COPY .mvn .mvn
COPY src ./src
RUN mvn -B -DskipTests package --fail-never

# Etapa de ejecución
FROM eclipse-temurin:21-jre
WORKDIR /app
ARG JAR_FILE=target/*.jar
COPY --from=build /workspace/${JAR_FILE} /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

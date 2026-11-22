FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests && \
    mv target/*.jar target/orquidea-backend.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /build/target/orquidea-backend.jar .

EXPOSE 8080

# Por defecto la JVM solo usa el 25 % de la memoria del contenedor; procesar fotos grandes necesita más
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75", "-jar", "orquidea-backend.jar"]

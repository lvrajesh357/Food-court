# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Cache dependencies separately from source so rebuilds are fast
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Railway injects PORT at runtime; application.properties already reads it.
EXPOSE 8989
# Small-instance tuning (Render free = 512 MB RAM, shared CPU): SerialGC + C1-only JIT start much faster
# and use far less memory; MaxRAMPercentage leaves headroom so the container is not killed (OOM).
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-XX:MaxRAMPercentage=75", "-XX:TieredStopAtLevel=1", "-jar", "app.jar"]

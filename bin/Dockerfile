# ============================
# BUILD STAGE
# ============================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline

# Copy source code
COPY src ./src

# Build the jar
RUN mvn -q -DskipTests package


# ============================
# RUNTIME STAGE
# ============================
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Copy jar from previous stage
COPY --from=build /app/target/*.jar app.jar

# Use production profile
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# Multi-stage Dockerfile for EduSync College Digital Platform
# Stage 1: Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy POM and download dependencies for faster cached builds
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production artifact
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root system user for container security
RUN addgroup -S edusync && adduser -S edusync -G edusync

# Copy compiled fat JAR from builder
COPY --from=builder /app/target/edusync-backend-1.0.0.jar app.jar

# Create uploads directory and grant permissions
RUN mkdir -p uploads && chown -R edusync:edusync /app

USER edusync

EXPOSE 8081

ENV PORT=8081 \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

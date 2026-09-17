# =====================================================
# 🇹🇿 National Cyber Security & Digital Forensics System
# Multi-stage Dockerfile — Production Ready
# Spring Boot 3.2.5 + Java 17 + PostgreSQL
# =====================================================

# ========== STAGE 1: BUILD ==========
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml kwanza (kwa Maven caching)
COPY pom.xml .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application (skip tests kwa kasi)
RUN mvn clean package -DskipTests

# ========== STAGE 2: RUNTIME ==========
FROM eclipse-temurin:17-jre-alpine

# Metadata
LABEL maintainer="JMK-YERE"
LABEL description="National Cyber Security & Digital Forensics System - Tanzania"
LABEL version="1.0.0"

# Install wget kwa health check
RUN apk add --no-cache wget

# Create non-root user (security best practice)
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Create uploads directory with proper permissions
RUN mkdir -p /app/uploads/evidence && \
    chown -R spring:spring /app

# Copy JAR kutoka build stage
COPY --from=build /app/target/*.jar app.jar

# Change ownership
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring:spring

# Expose port (Render itatumia PORT variable)
EXPOSE 8080

# JVM options kwa memory efficiency (Render free tier = 512 MB)
ENV JAVA_OPTS="-Xmx400m -Xms256m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m"
ENV SERVER_PORT=8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -q -O /dev/null http://localhost:8080/login || exit 1

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=$SERVER_PORT -jar app.jar"]

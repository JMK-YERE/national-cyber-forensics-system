# ========== STAGE 1: BUILD ==========
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Resolve dependencies once during verification; the separate go-offline pass
# duplicates Maven work on Render builds where Docker layer caching is disabled.
RUN mvn -B -ntp clean verify

# ========== STAGE 2: RUNTIME ==========
FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache wget curl

RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
RUN mkdir -p /app/uploads/evidence && chown -R spring:spring /app

COPY --from=build /app/target/*.jar app.jar
RUN chown spring:spring app.jar
USER spring:spring

EXPOSE 8080

# JVM options — faster startup, less memory
ENV JAVA_OPTS="-Xmx400m -Xms128m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:MaxMetaspaceSize=128m -Dspring.jmx.enabled=false -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=$PORT -jar app.jar"]

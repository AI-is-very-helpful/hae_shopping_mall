# Multi-stage build for HAE Shop
# Stage 1: Build
FROM gradle:8.11-jdk21 AS builder

WORKDIR /app

COPY build.gradle settings.gradle gradle/ ./

RUN gradle dependencies --no-daemon || true

COPY src ./src

RUN gradle bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar
COPY --from=builder /app/src/main/resources ./src/main/resources

EXPOSE 8080

ENV JAVA_OPTS="\
  -XX:+UseZGC \
  -XX:+AlwaysPreTouch \
  -XX:-UseBiasedLocking \
  -Djdk.tracePinnedThreads=full"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

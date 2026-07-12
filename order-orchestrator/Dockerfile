# syntax=docker/dockerfile:1

FROM gradle:8.10.2-jdk21 AS builder
WORKDIR /workspace

COPY settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY common-libs ./common-libs
COPY order-orchestrator ./order-orchestrator

RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle --no-daemon :order-orchestrator:bootJar --configuration-cache --build-cache -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
ENV JAVA_OPTS=""
COPY --from=builder /workspace/order-orchestrator/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

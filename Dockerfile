# ---- Build stage -----------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests clean package

# ---- Runtime stage ---------------------------------------------------------
FROM eclipse-temurin:21-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd -m -u 1000 runner

USER runner
WORKDIR /app

COPY --from=build /app/target/CodeArena-1.0.0.jar app.jar

# Workspaces must live under a path that is identical on the host and inside
# this container so the sandbox containers can bind-mount them. Mount a host
# volume at /var/lib/codearena/workspaces and set WORKSPACE_ROOT accordingly.
ENV WORKSPACE_ROOT=/var/lib/codearena/workspaces \
    SERVER_PORT=8081 \
    JAVA_OPTS=""

RUN mkdir -p /var/lib/codearena/workspaces \
    && chown runner:runner /var/lib/codearena/workspaces

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -fsS http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# syntax=docker/dockerfile:1
# =============================================================================
# API da Plataforma -- Documento Mestre V4.0, secao 23:
#   "Backend: conteiner em Railway ou Fly.io."
#
# Multi-estagio para que a imagem final nao carregue Maven, codigo-fonte nem o
# repositorio ~/.m2 -- tudo isso e superficie de ataque e centenas de MB que
# nao servem para nada em producao.
# =============================================================================

# --- Estagio 1: build --------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /construcao

COPY pom.xml .
COPY src ./src

# Cache mount do BuildKit no ~/.m2 em vez de dependency:go-offline. Duas razoes:
# go-offline nao resolve os artefatos de plugin do Spring Boot e falha; e o cache
# mount sobrevive a mudanca do pom, entao acrescentar uma dependencia nao obriga
# a rebaixar todas as outras.
# Os testes rodam no CI (secao 24). Repeti-los aqui so aumentaria o tempo de
# build da imagem sem acrescentar garantia nenhuma.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B clean package -DskipTests

# --- Estagio 2: execucao -----------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

# Usuario sem privilegio. Um processo comprometido rodando como root dentro do
# conteiner e o primeiro degrau para escapar dele.
RUN addgroup -S plataforma && adduser -S plataforma -G plataforma

WORKDIR /app
COPY --from=build /construcao/target/*.jar app.jar
RUN chown -R plataforma:plataforma /app
USER plataforma

EXPOSE 8080

# MaxRAMPercentage em vez de -Xmx fixo: a JVM le o limite de memoria do proprio
# conteiner, entao a mesma imagem se ajusta ao plano que o Railway/Fly entregar.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Duser.timezone=UTC"

# Secao 23: "Uptime: checagem externa a cada minuto em home, login e webhook."
# Este healthcheck e o de dentro do orquestrador, que decide quando o conteiner
# esta pronto para receber trafego.
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health/readiness | grep -q UP || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

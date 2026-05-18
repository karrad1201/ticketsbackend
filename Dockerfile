# ── Stage 1: build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:24-jdk-alpine AS builder

RUN apk add --no-cache curl

WORKDIR /build

# Cache Maven dependencies before copying source
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline -q

COPY src/ src/
RUN ./mvnw -B -DskipTests package -q

# ── Stage 2: runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:24-jre-alpine AS runtime

RUN addgroup -S bilets && adduser -S bilets -G bilets

WORKDIR /app
RUN chown bilets:bilets /app
USER bilets

COPY --from=builder /build/target/bilets-*.jar app.jar
RUN mkdir -p uploads

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

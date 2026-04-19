# Deployment Guide

## Prerequisites

- Java 24+
- PostgreSQL 15+
- Redis 7+
- Maven 3.9+ (или используйте встроенный `./mvnw`)

---

## Переменные окружения

Все переменные ниже используются профилем `prod` (`application-prod.yml`).
Без них приложение не запустится в production-режиме.

| Переменная              | Обязательна | Default | Описание                                                                  |
|-------------------------|:-----------:|---------|---------------------------------------------------------------------------|
| `REDIS_URL`             | да          | —       | URL подключения к Redis, например `redis://redis:6379`                    |
| `DATASOURCE_URL`        | да          | —       | JDBC URL PostgreSQL, например `jdbc:postgresql://postgres:5432/bilets`    |
| `DATASOURCE_USERNAME`   | да          | —       | Имя пользователя PostgreSQL                                               |
| `DATASOURCE_PASSWORD`   | да          | —       | Пароль пользователя PostgreSQL                                            |
| `CORS_ALLOWED_ORIGINS`  | да          | —       | Список разрешённых CORS-origins, например `https://example.com`           |
| `TBANK_TERMINAL_KEY`    | да          | —       | Terminal key для интеграции с T-Bank (Tinkoff Pay)                        |
| `TBANK_PASSWORD`        | да          | —       | Пароль для подписи запросов к T-Bank                                      |
| `TBANK_NOTIFICATION_URL`| да          | —       | URL, на который T-Bank отправляет payment callbacks                       |
| `ZVONOK_PUBLIC_KEY`     | да          | —       | Публичный ключ для интеграции со Zvonok (звонковые уведомления)           |
| `ZVONOK_CAMPAIGN_ID`    | да          | —       | Идентификатор кампании в системе Zvonok                                   |

> В prod-профиле Swagger UI и OpenAPI docs отключены (`springdoc.swagger-ui.enabled: false`, `springdoc.api-docs.enabled: false`).

---

## Профили Spring

Приложение поддерживает четыре конфигурационных профиля.

### (default) — H2 + JDBC order flow

Профиль по умолчанию, если не указан ни один активный профиль.
Использует встроенную H2-базу данных с Flyway-миграциями и JDBC-хранилище для order flow.
Подходит для разработки и CI-тестирования.

```bash
SPRING_DATASOURCE_URL=jdbc:h2:mem:bilets;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false \
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver \
SPRING_DATASOURCE_USERNAME=sa \
SPRING_DATASOURCE_PASSWORD= \
./mvnw spring-boot:run
```

### `in-memory` — полностью in-memory, без JDBC

Профиль для лёгкой локальной разработки без какой-либо базы данных.
DataSource и Flyway полностью отключены. Order flow хранится в памяти процесса.

```bash
SPRING_PROFILES_ACTIVE=in-memory ./mvnw spring-boot:run
```

### `jdbc-order-flow` — JDBC для order flow, H2 для остального

Профиль включает JDBC-бэкенд конкретно для order flow при сохранении H2 для остальных сущностей.
Используется для изолированного тестирования persistence-слоя заказов.

```bash
SPRING_PROFILES_ACTIVE=jdbc-order-flow ./mvnw spring-boot:run
```

### `prod` — PostgreSQL + Redis

Боевой профиль. Использует PostgreSQL в качестве основной БД и Redis для кэширования сессий покупки.
Swagger UI отключён. Все параметры подключения берутся из переменных окружения (см. таблицу выше).

```bash
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

---

## Running Locally

Запуск в профиле `in-memory` (без внешних зависимостей):

```bash
SPRING_PROFILES_ACTIVE=in-memory ./mvnw spring-boot:run
```

Запуск в дефолтном профиле (H2 + JDBC):

```bash
SPRING_DATASOURCE_URL=jdbc:h2:mem:bilets;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false \
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver \
SPRING_DATASOURCE_USERNAME=sa \
SPRING_DATASOURCE_PASSWORD= \
./mvnw spring-boot:run
```

Запуск с профилем `jdbc-order-flow`:

```bash
SPRING_PROFILES_ACTIVE=jdbc-order-flow ./mvnw spring-boot:run
```

Запуск с профилем `prod` (требует запущенных PostgreSQL и Redis):

```bash
SPRING_PROFILES_ACTIVE=prod \
REDIS_URL=redis://localhost:6379 \
DATASOURCE_URL=jdbc:postgresql://localhost:5432/bilets \
DATASOURCE_USERNAME=bilets \
DATASOURCE_PASSWORD=secret \
CORS_ALLOWED_ORIGINS=http://localhost:3000 \
TBANK_TERMINAL_KEY=your_terminal_key \
TBANK_PASSWORD=your_password \
TBANK_NOTIFICATION_URL=https://your-domain.com/api/payments/callbacks/tbank \
ZVONOK_PUBLIC_KEY=your_public_key \
ZVONOK_CAMPAIGN_ID=your_campaign_id \
./mvnw spring-boot:run
```

---

## Docker

Пример `docker-compose.yml` для запуска всего стека в production-режиме:

```yaml
version: "3.9"

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: bilets
      POSTGRES_USER: bilets
      POSTGRES_PASSWORD: secret
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U bilets -d bilets"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    image: eclipse-temurin:24-jre-alpine
    working_dir: /app
    volumes:
      - ./target/bilets-0.0.1-SNAPSHOT.jar:/app/app.jar
    command: ["java", "-jar", "app.jar"]
    environment:
      SPRING_PROFILES_ACTIVE: prod
      REDIS_URL: redis://redis:6379
      DATASOURCE_URL: jdbc:postgresql://postgres:5432/bilets
      DATASOURCE_USERNAME: bilets
      DATASOURCE_PASSWORD: secret
      CORS_ALLOWED_ORIGINS: https://your-frontend-domain.com
      TBANK_TERMINAL_KEY: your_terminal_key
      TBANK_PASSWORD: your_tbank_password
      TBANK_NOTIFICATION_URL: https://your-domain.com/api/payments/callbacks/tbank
      ZVONOK_PUBLIC_KEY: your_zvonok_public_key
      ZVONOK_CAMPAIGN_ID: your_campaign_id
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "wget -qO- http://localhost:8080/actuator/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

volumes:
  postgres_data:
  redis_data:
```

Сборка JAR перед запуском:

```bash
./mvnw -B -DskipTests package
docker compose up -d
```

---

## Health Checks

Приложение экспортирует эндпоинты Spring Boot Actuator. Открытые эндпоинты настраиваются в `application.yml` (`management.endpoints.web.exposure.include: health,info,metrics`).

### `GET /actuator/health`

Возвращает агрегированный статус работоспособности приложения.

```json
{
  "status": "UP"
}
```

С атрибутом `show-details: when_authorized` авторизованные запросы получают детальную информацию по каждому компоненту (БД, Redis, дисковое пространство и т.д.). Неавторизованные запросы видят только поле `status`.

### `GET /actuator/metrics`

Возвращает список доступных метрик приложения (JVM, HTTP-запросы, пул соединений и пр.).

```json
{
  "names": ["jvm.memory.used", "http.server.requests", "hikaricp.connections", ...]
}
```

Конкретную метрику можно запросить по имени:

```
GET /actuator/metrics/jvm.memory.used
GET /actuator/metrics/http.server.requests
```

---

## Flyway Migrations

Миграции базы данных расположены в `src/main/resources/db/migration` и запускаются **автоматически при старте** приложения во всех профилях, где активен DataSource (то есть во всех, кроме `in-memory`).

Flyway использует стратегию **forward-only**: откат миграции через Flyway невозможен. Для исправления ошибочной миграции необходимо создать новый скрипт, который отменяет нежелательные изменения.

Соглашение об именовании файлов миграций: `V<version>__<description>.sql`, например `V1__init_schema.sql`.

---

## CI/CD

Пайплайн описан в `.github/workflows/ci-cd.yml` и состоит из двух джобов.

### `ci` — Test And Coverage

Запускается на каждый `push` в ветки `main` и `feature/**`, а также на каждый Pull Request в `main`.

1. Поднимает сервис Redis (`redis:7-alpine`) как sidecar-контейнер с health-check.
2. Устанавливает JDK 24 (Temurin) с кэшированием Maven-зависимостей.
3. Запускает `mvn -B test jacoco:report jacoco:check@check` — тесты + проверка покрытия.
   - Минимальное покрытие строк domain-пакета: **90%**.
   - Минимальное покрытие строк и ветвей use case-пакета: **80%**.
4. Загружает артефакты: Surefire-отчёты и HTML-отчёт JaCoCo.

### `cd` — Build Delivery Artifact

Запускается только при `push` в `main` после успешного прохождения джоба `ci`.

1. Собирает JAR: `mvn -B -DskipTests package`.
2. Загружает собранный JAR как артефакт `bilets-jar` (исключая `-sources.jar`).

> Деплой на сервер в пайплайн не включён — загруженный артефакт предназначен для последующей ручной или внешней доставки.

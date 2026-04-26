JAVA_HOME ?= /home/karrad/.jdks/openjdk-26
MAVEN_BIN ?= /opt/intellij-idea-ultimate-edition/plugins/maven/lib/maven3/bin
PATH := $(MAVEN_BIN):$(JAVA_HOME)/bin:$(PATH)

IMAGE  ?= karradm/bilets-backend
TAG    ?= latest

COMPOSE_DEVSTACK = docker compose -f docker-compose.devstack.yml

.PHONY: test coverage tree install-hooks \
        devstack-build devstack-up devstack-down devstack-restart \
        devstack-logs devstack-push devstack-psql devstack-redis-flush devstack-sms

# ── Tests ──────────────────────────────────────────────────────────────────────

test:
	mvn test

coverage:
	mvn test jacoco:report jacoco:check@check

# ── Dev tooling ────────────────────────────────────────────────────────────────

tree:
	./scripts/generate_tree.py

install-hooks:
	git config core.hooksPath .githooks

# ── DevStack ───────────────────────────────────────────────────────────────────
# Prod-like стек: nginx (lb) → app1 + app2 → postgres + redis
# Единственный открытый порт: 80 (nginx).
# API: http://localhost/api/...
# Swagger UI: http://localhost/swagger-ui/index.html
#
# Предзасеянные токены (Authorization: Bearer <token>):
#   devstack-admin-token  — ADMIN
#   devstack-owner-token  — Org OWNER (+79991000002)
#   devstack-staff-token  — Org STAFF (+79991000003)
#   devstack-user-token   — обычный пользователь
# SMS-код для всех номеров: 123456

## Собрать Docker-образ из исходников
devstack-build:
	./mvnw -B -DskipTests package -q
	docker build -t $(IMAGE):$(TAG) .

## Запустить весь стек в фоне
devstack-up:
	$(COMPOSE_DEVSTACK) up -d

## Остановить и удалить контейнеры (данные volumes сохраняются)
devstack-down:
	$(COMPOSE_DEVSTACK) down

## Перезапустить только app1 и app2 (без пересоздания БД)
devstack-restart:
	$(COMPOSE_DEVSTACK) restart app1 app2

## Показать логи всех сервисов (follow)
devstack-logs:
	$(COMPOSE_DEVSTACK) logs -f

## Запушить образ в Docker Hub (требует docker login)
devstack-push:
	docker push $(IMAGE):$(TAG)

## Открыть psql в postgres-контейнере
devstack-psql:
	$(COMPOSE_DEVSTACK) exec postgres psql -U bilets -d bilets

## Сбросить Redis-кэш
devstack-redis-flush:
	$(COMPOSE_DEVSTACK) exec redis redis-cli FLUSHALL

## Показать SMS-коды из логов (MockSmsGateway пишет номер телефона)
devstack-sms:
	$(COMPOSE_DEVSTACK) logs app1 app2 | grep "MOCK SMS"

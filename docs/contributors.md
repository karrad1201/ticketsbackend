# Contributors Guide

## Development Flow

Работаем короткими feature-ветками, а не напрямую в `main`.

Базовый цикл:

1. Обновить локальный `main`.
2. Создать ветку вида `feature/<short-name>`.
3. Делать изменения по `TDD`: тесты, код, рефакторинг.
4. Для неочевидных архитектурных решений добавить или обновить `ADR`.
5. Перед пушем прогнать:
   - `make test`
   - `make coverage`
6. Обновить дерево проекта:
   - `make tree`
7. Запушить feature-ветку.
8. Открыть `PR` в `main`.
9. Мержить только после зеленого `CI`.
10. После merge удалить feature-ветку и начинать следующую задачу из свежего `main`.

## Repository Hooks

- Один раз на клон:
  - `make install-hooks`
- Это включает локальный `.githooks/post-commit`.
- `post-commit` автоматически регенерирует [tree.md](/home/karrad/IdeaProjects/ticketsbackend/docs/tree.md) из tracked files через `scripts/generate_tree.py`.
- Hook намеренно игнорирует мусор вроде `.git`, `target`, `.idea`, потому что источник дерева это `git ls-files`.
- Так как hook срабатывает после commit, `docs/tree.md` может остаться измененным в рабочем дереве. Если нужно включить актуальное дерево в текущий commit, запускаем `make tree` до commit.

## Branch Policy

- `main` должен оставаться рабочим и проходить тесты.
- Новая задача или slice идут в новой ветке.
- Нельзя смешивать несколько независимых задач в одном `PR`.
- Если работа большая, режем ее на несколько последовательных `PR`, а не в один крупный коммит.

## Testing Expectations

- Домен: высокий unit coverage, особенно для инвариантов.
- `use case`: покрываем success, reject, duplicate, expired, race и idempotency ветки, если они есть.
- Инфраструктура: высокий уровень integration tests с mock-зависимостями.
- Web/e2e: только ключевые пользовательские сценарии и критичные ошибки.

## CI/CD

Минимальный pipeline живет в `.github/workflows/ci-cd.yml`.

- `CI` запускается на `push` в `main` и `feature/**`, а также на `PR` в `main`.
- `CI` прогоняет тесты и coverage gates.
- `CD` в текущем минимальном виде не деплоит сервис, а собирает delivery artifact на `push` в `main` и публикует `jar` как GitHub Actions artifact.

Это сознательно минимальный уровень automation, пока у проекта нет целевого deployment environment.

## Runtime Modes

- Основной contour развивается вокруг JDBC runtime и Flyway migrations.
- `in-memory` оставлен как явный профиль для быстрой локальной разработки и дешевых тестовых сценариев.
- Новую persistence-логику не добавляем silently в default in-memory wiring. Если aggregate уже важен для durable path, приоритет у JDBC-адаптера и integration tests вокруг него.

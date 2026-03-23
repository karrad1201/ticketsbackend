# ticketsbackend

Это spring boot бекенд платформы по продаже билетов на мероприятия.
Это проект уровня ближе к федеральному - он не привязан к городу или субъекту.
Мы позиционируем себя как платформа для туризма и досуга

Основные пользователи:
A: Люди, желающие найти какое-либо развлечение в своем городе
Б: Кураторы, которые хотят продать билет на своё мероприятие

__

Интересное замечание - мы продаем билеты как на мероприятие с позиционированием мест, так и без привязки к какому-либо месту



--
Разработка должна вестись по tdd системе, общий вид: Тесты-Код-Рефакторинг. 
Важно делать коммиты и неочевидные решения прописывать в adr

Работа по фичам ведется через отдельные ветки и `PR`, а не напрямую в `main`.
Подробный процесс разработки вынесен в [docs/contributors.md](docs/contributors.md).

## Development Tooling

- Перед первой работой в клоне установить локальные git hooks:
  - `make install-hooks`
- Генерация дерева проекта:
  - `make tree`
- `post-commit` hook обновляет [docs/tree.md](docs/tree.md) после каждого локального коммита.
- Генератор дерева явно игнорирует `target/`, `.idea/`, `.git/` и сам `docs/tree.md`.
  Это намеренно оставляет обновленное дерево в рабочем дереве. Если нужно включить его в текущий commit, запускаем `make tree` до commit.


Стандартный флоу добавления фич:

1. Изменение доменной модели
2. Создание или изменение CRUD-интерфейса репозитория
3. Создание или изменение application service
4. Создание in-memory реализации репозитория
5. Написание и прогон тестов на CRUD/application-каркас
6. Написание тестов для use case
7. Реализация use case
8. Рефакторинг use case, по необходимости ADR и коммит
9. Написание интеграционных тестов
10. Написание контроллера
11. Финальный прогон тестов, рефакторинг при необходимости, коммит. Новая фича введена

## Persistence Modes

Основной runtime теперь рассчитан на JDBC-backed запуск.
По умолчанию приложение поднимает H2 datasource и применяет Flyway migrations из `src/main/resources/db/migration`.

Что сейчас уже работает в JDBC contour:

- `User`, `Organization`, `OrganizationApplication`, `Category`, `OrganizationMember`;
- `Venue`, `LayoutTemplate`, `Event`, `EventInventoryPlan`, `UserEventVisit`;
- `Order`, `Ticket`, `OrderInventory`;
- `PaymentAttempt`, `PaymentCallbackAudit`.

Минимальный запуск default режима:

```bash
SPRING_DATASOURCE_URL=jdbc:h2:mem:bilets;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false \
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver \
SPRING_DATASOURCE_USERNAME=sa \
SPRING_DATASOURCE_PASSWORD= \
./mvnw spring-boot:run
```

Для легкой локальной разработки без datasource есть отдельный профиль `in-memory`:

```bash
SPRING_PROFILES_ACTIVE=in-memory ./mvnw spring-boot:run
```

JDBC contour сейчас покрывает durable purchase slice и его ближайшие зависимости:

- ownership check через `OrganizationMember`;
- organization application storage и review flow;
- category lookup для event creation;
- venue-backed event creation;
- layout template storage для seated flow;
- visit history storage для discovery;
- inventory generation;
- order creation, confirm, expire и ticket issuance;
- payment callback audit и reconciliation-ready `PaymentAttempt`.

## Payment Flow

Пока нет реальной внешней PSP-интеграции, но бизнес-модель оплаты уже отделена от order lifecycle:

- `PaymentGateway` отвечает за создание payment session;
- `PaymentAttempt` хранит попытку оплаты и ее статус;
- `HandlePaymentCallbackUseCase` обрабатывает внешнее подтверждение/ошибку;
- `PaymentCallbackAudit` хранит входящие callback события;
- `PaymentReconciliationService` позволяет находить устаревшие pending попытки.

Для тестового и локального контура есть mock callback endpoint:

- `POST /api/payments/callbacks/mock`

Он нужен для проверки внешнего confirm/fail пути без реальной платежки.

## Identity Boundary

Публичные write/read endpoints больше не принимают `userId` как источник истины.
Минимальная boundary сейчас такая:

- текущий пользователь берется из `X-User-Id`;
- admin endpoints проверяют, что этот пользователь действительно `ADMIN`;
- `Order`, `Ticket`, `Discovery`, `Venue`, `Event`, `LayoutTemplate`, `OrganizationApplication` используют именно current user context.

Это еще не полноценная auth-система, а минимальный шаг, чтобы прекратить доверять произвольному `userId` из тела запроса.

# ADR 0007: Introduce JDBC-Backed Durable Purchase Slice

## Status

Accepted

## Context

Purchase flow перестал помещаться в гарантии in-memory модели:

- `make order` должен атомарно резервировать inventory;
- `confirm payment` и `expire order` должны безопасно конкурировать;
- `seat` не должен продаваться дважды;
- `general admission` не должен oversell-иться под конкурентной нагрузкой;
- rollback при частичном reserve должен быть транзакционным.

Process-local lock на `ConcurrentHashMap` решает только часть задачи:

- работает внутри одного JVM-процесса;
- не дает durable state;
- не переносится на несколько инстансов;
- не является источником истины для inventory и order state machine.

## Decision

Для purchase flow вводим JDBC-backed путь с транзакциями и SQL-атомарностью.

Новые опоры решения:

- `OrderFlowTransactionManager` как application-level контракт транзакции;
- `OrderInventoryRepository` как отдельный порт для reserve/confirm/release inventory под purchase flow;
- JDBC-реализации для:
  - `UserRepository`
  - `OrganizationRepository`
  - `OrganizationApplicationRepository`
  - `CategoryRepository`
  - `OrganizationMemberRepository`
  - `VenueRepository`
  - `LayoutTemplateRepository`
  - `EventRepository`
  - `UserEventVisitRepository`
  - `EventInventoryPlanRepository`
  - `OrderRepository`
  - `OrderInventoryRepository`
  - `TicketRepository`

Схема:

- `event_inventory_plans`
- `event_seat_inventory`
- `event_admission_inventory`
- `orders`
- `order_seat_items`
- `order_admission_items`
- `tickets`

Конкурентная модель:

- reserve seat делается через `update ... where status = 'AVAILABLE'`;
- reserve general admission делается через `update ... where capacity - sold - held >= qty`;
- `confirm` и `expire` читают заказ через row lock (`findByIdForUpdate`);
- order lifecycle оборачивается в DB transaction;
- rollback частичного reserve обеспечивается транзакцией, а не compensating code.

На первом этапе это включалось отдельным Spring profile:

- `jdbc-order-flow`

Дальше это решение было расширено: JDBC contour стал целевым default runtime, а `in-memory` остался отдельным явным dev-profile.
Схема хранится в versioned migrations:

- `db/migration/V1__jdbc_order_flow.sql`
- `db/migration/V2__payment_model.sql`

Применение схемы теперь идет через Flyway, а не через кастомный initializer.

После расширения профиля durable purchase path включает не только сам `order flow`, но и ближайшие ownership/catalog зависимости:

- membership-проверку через `OrganizationMember`;
- lifecycle `OrganizationApplication`;
- category lookup через `Category`;
- создание `Venue`;
- хранение `LayoutTemplate` для seated inventory;
- создание `Event` поверх `Venue`;
- хранение `UserEventVisit` для discovery/read path;
- inventory generation перед покупкой.

## Consequences

Плюсы:

- purchase flow перестает зависеть от process-local mutex как основной гарантии корректности;
- появляются durable order/inventory/ticket данные;
- ownership и venue-backed event creation тоже начинают жить в том же persistence family;
- admin flow вокруг organization application тоже начинает жить в JDBC contour;
- event creation в JDBC contour больше не опирается на in-memory `CategoryRepository`;
- seated catalog перестает зависеть от in-memory `LayoutTemplateRepository` в JDBC contour;
- discovery path начинает читать visit history из JDBC, а не из in-memory адаптера;
- concurrency и rollback теперь проверяются тестами против реальной БД;
- JDBC становится основным направлением runtime, а `in-memory` остается осознанным режимом для дешевой локальной разработки.

Минусы:

- проект временно живет в смешанном режиме: durable order flow и часть остальной системы все еще разделены по persistence strategy;
- переход к JDBC как default runtime делает локальный dev без БД менее удобным без явного `in-memory` profile;
- payment model и order lifecycle начинают жить как связанные, но разные state machine.

## Next Step

Следующий технический шаг:

1. перевести оставшиеся aggregate/repository на JDBC или явные adapter boundaries;
2. ввести отдельный `PaymentAttempt` lifecycle, callback handling и reconciliation;
3. убрать `userId` из публичных write/read endpoint как источник истины;
4. добавить production datasource вместо локального H2 сценария;
5. при необходимости вынести scheduler на массовый `expire pending orders`.

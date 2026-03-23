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

В runtime это включается отдельным Spring profile:

- `jdbc-order-flow`

Профиль переводит на JDBC только те части, которые уже нужны для durable purchase path.
Схема хранится как versioned migration script `db/migration/V1__jdbc_order_flow.sql` и применяется при старте JDBC-профиля через initializer.

После расширения профиля durable purchase path включает не только сам `order flow`, но и ближайшие ownership/catalog зависимости:

- membership-проверку через `OrganizationMember`;
- lifecycle `OrganizationApplication`;
- category lookup через `Category`;
- создание `Venue`;
- хранение `LayoutTemplate` для seated inventory;
- создание `Event` поверх `Venue`;
- inventory generation перед покупкой.

## Consequences

Плюсы:

- purchase flow перестает зависеть от process-local mutex как основной гарантии корректности;
- появляются durable order/inventory/ticket данные;
- ownership и venue-backed event creation тоже начинают жить в том же persistence family;
- admin flow вокруг organization application тоже начинает жить в JDBC profile;
- event creation в JDBC profile больше не опирается на in-memory `CategoryRepository`;
- seated catalog перестает зависеть от in-memory `LayoutTemplateRepository` в JDBC profile;
- concurrency и rollback теперь проверяются тестами против реальной БД;
- profile можно включать отдельно, не ломая default in-memory режим разработки.

Минусы:

- проект временно живет в смешанном режиме: durable order flow и часть остальной системы все еще разделены по persistence strategy;
- `jdbc-order-flow` пока не делает весь backend полностью JDBC-backed;
- migration script уже versioned, но отдельный migration framework пока не подключен.

## Next Step

Следующий технический шаг:

1. перевести оставшиеся purchase-adjacent aggregate/repository на JDBC или явные adapter boundaries;
2. ввести полноценный migration framework поверх versioned script;
3. добавить production datasource вместо локального H2 сценария;
4. при необходимости вынести scheduler на массовый `expire pending orders`.

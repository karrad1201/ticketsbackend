# Current State

## Runtime

- Основной runtime: `JDBC + Flyway + H2`.
- Легкий dev contour: профиль `in-memory`.
- `target/` больше не считается частью репозитория и игнорируется через `.gitignore`.

## Main Product Flow

1. `User` подает `OrganizationApplication`.
2. `Admin` ревьюит заявку.
3. При approve создаются `Organization` и owner `OrganizationMember`.
4. Организация создает `Venue`, `LayoutTemplate`, `Event`.
5. Для `Event` генерируется inventory.
6. Покупатель создает `Order`, reserve inventory и получает `PaymentAttempt`.
7. Оплата подтверждается через direct confirm или mock callback.
8. При успехе inventory подтверждается, выпускаются `Ticket`, баланс организации кредитуется.

## Important Boundaries

- Public web layer использует `X-User-Id` как временный current-user контракт.
- Полноценной auth/authz пока нет.
- Основные purchase guarantees держатся на JDBC transaction + SQL atomic updates.

## Purchase And Event Sales

- `PaymentCallbackStatus.EXPIRED` теперь освобождает inventory и завершает pending order неуспешно.
- `confirm-payment` после истечения payment window тоже release'ит inventory и fail'ит `PaymentAttempt`.
- У `Event` есть состояние закрытия продаж через `salesClosedAt`.
- Продажи закрываются:
  - вручную через organizer action;
  - автоматически по бизнес-правилу, когда наступило время события.
- Закрытие продаж события теперь также завершает все pending purchase attempts по этому событию.

## Read Models

- `search` и `discovery` отдают только события, доступные для покупки.
- Для JDBC runtime `search` и `discovery` теперь идут через целевые repository query path, а не через глобальный `findAll()`.
- Отдельного read-optimized projection слоя пока нет: это еще не полноценная search/discovery read model.

## Operational Flows

- Есть `PaymentReconciliationService` для поиска stale pending attempts.
- Есть `ProcessStalePaymentAttemptsUseCase` для их пакетной обработки через стандартный expire path.
- Есть `ProcessStartedEventSalesUseCase` для пакетного auto-close начавшихся событий.
- Есть scheduler для auto-close started event sales и stale payment processing.
- Есть admin ops HTTP surface для ручного запуска обоих batch flow.

## Current Technical Limits

- Approval flow организации и прочие multi-step бизнес-переходы еще не везде одинаково богаты operational tooling.
- Search/discovery пока не вынесены в отдельные SQL/read-optimized projection модели.
- Security boundary остается минимальной до отдельного auth/authz slice.

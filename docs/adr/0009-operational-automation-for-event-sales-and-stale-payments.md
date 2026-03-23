# ADR 0009: Add Operational Automation For Event Sales And Stale Payments

## Status

Accepted

## Context

После ввода durable purchase flow и lifecycle оплаты в системе появились два важных operational сценария:

- started events должны автоматически закрывать продажи и завершать pending purchases;
- stale pending payments должны автоматически проходить через expire path, а не ждать ручного вызова use case.

До этого система умела выполнять оба перехода только как локальную бизнес-логику, но не имела нормального operational запуска.

## Decision

Вводим отдельный operational слой:

- `ProcessStartedEventSalesUseCase` для пакетного закрытия начавшихся событий;
- `ProcessStalePaymentAttemptsUseCase` для пакетной обработки stale pending payments;
- `OperationsScheduler` для автоматического запуска обоих batch flow;
- `OperationsController` как admin-only HTTP surface для ручного запуска:
  - `POST /api/ops/close-started-event-sales`
  - `POST /api/ops/process-stale-payments`
- `operations.*` properties для настройки batch size и scheduler delay.

Дополнительно закрытие продаж события переводим под тот же `EventLockManager`, что и purchase flow, чтобы manual close, scheduled close и payment/order transitions не расходились по состоянию.

## Consequences

Плюсы:

- started events и stale payments больше не требуют ручного кода для регулярной обработки;
- admin получает явный ops surface для ручного дожима системы;
- event sales closure и purchase transitions остаются сериализованными на одном event-level lock;
- бизнес-инварианты выполняются не только в happy-path use case, но и в operational contour.

Минусы:

- `@Scheduled` остается простым in-process механизмом без distributed coordination;
- нет execution audit и retries на уровне job runtime;
- operations layer пока не моделирует job history или явный failure queue.

## Next Step

1. добавить execution audit и метрики по operational batch runs;
2. при необходимости вынести scheduler/jobs в более явную execution model;
3. при multi-instance runtime решить distributed scheduling/locking strategy.

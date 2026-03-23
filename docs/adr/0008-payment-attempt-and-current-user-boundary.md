# ADR 0008: Introduce Payment Attempt Lifecycle and Current User Boundary

## Status

Accepted

## Context

После перевода purchase core на durable JDBC contour возникли два архитектурных разрыва:

- оплата все еще моделировалась слишком грубо: `create payment` существовал, но внешнее подтверждение/ошибка оплаты не были отдельной частью доменной модели;
- публичные web endpoints продолжали доверять `userId` из request body или query params, что противоречит даже минимальной security boundary.

Для production-grade purchase flow этого уже недостаточно:

- нужен отдельный lifecycle попытки оплаты;
- нужен idempotent callback path;
- нужен audit входящих callback-событий;
- текущий пользователь должен определяться из identity context, а не из произвольного поля запроса.

## Decision

Вводим следующие опоры:

- `PaymentAttempt` как доменную сущность попытки оплаты;
- `PaymentAttemptStatus` со статусами `PENDING`, `SUCCEEDED`, `FAILED`;
- `PaymentCallbackAudit` для фиксации входящих callback-событий;
- `HandlePaymentCallbackUseCase` как отдельный внешний confirm/fail путь;
- `PaymentReconciliationService` для поиска устаревших pending attempts;
- `CurrentUserProvider` как минимальную identity boundary для web-слоя.

Из этого следуют конкретные изменения:

- `CreateOrderUseCase` создает не только `Order`, но и `PaymentAttempt`;
- `ConfirmOrderPaymentUseCase` и callback path работают через один settlement слой, а не дублируют post-payment логику;
- `ExpireOrderUseCase` помечает pending payment attempt как failed, если hold истек;
- web-слой использует `X-User-Id` как временный current-user контракт;
- admin endpoints проверяют роль текущего пользователя через `CurrentUserProvider.requireAdmin()`;
- `Order`, `Ticket`, `Discovery`, `Venue`, `Event`, `LayoutTemplate`, `OrganizationApplication` больше не получают actor user из request payload.

## Consequences

Плюсы:

- payment lifecycle отделяется от order lifecycle, но остается связанным через явные state transitions;
- появляется idempotent callback path для mock payment provider и будущей реальной интеграции;
- появляется audit trail по входящим callback-событиям;
- reconciliation перестает быть ad-hoc логикой внутри order flow;
- web boundary становится честнее: система перестает доверять произвольному `userId` из запроса.

Минусы:

- модель становится сложнее: теперь есть и `OrderStatus`, и `PaymentAttemptStatus`;
- `X-User-Id` это только временный current-user контракт, а не полноценная authentication system;
- для реальной PSP интеграции все еще понадобятся дополнительные статусы, подписи callback и provider-specific semantics.

## Next Step

1. расширить payment model до `PaymentProvider`-ready контракта с webhook/idempotency semantics;
2. ввести полноценный auth/authz слой вместо `X-User-Id` header;
3. добавить явные settlement/audit read models для финансовых сценариев;
4. при необходимости вынести callback handling и reconciliation в отдельные operational flows.

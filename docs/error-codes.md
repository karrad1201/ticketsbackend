# Error Codes & HTTP Status Reference

## Формат ответа при ошибке

API использует стандарт [RFC 7807 Problem Details](https://www.rfc-editor.org/rfc/rfc7807) через Spring's `ProblemDetail`.
При любой ошибке тело ответа содержит следующие поля:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Phone already registered: +79001234567",
  "instance": "/auth/register"
}
```

| Поле       | Тип    | Описание                                                           |
|------------|--------|--------------------------------------------------------------------|
| `type`     | string | URI типа ошибки (по умолчанию `about:blank`)                       |
| `title`    | string | Краткое название HTTP-статуса                                      |
| `status`   | int    | HTTP-статус код                                                    |
| `detail`   | string | Человекочитаемое описание конкретной ошибки                        |
| `instance` | string | Путь запроса, при котором возникла ошибка                          |

> **Важно:** Отдельного поля `code` с domain-специфичным кодом в текущей реализации нет.
> Смысл ошибки передаётся через поле `detail` в виде текста.

---

## HTTP-статусы

### 400 Bad Request

Возникает при нарушении бизнес-правил или некорректных входных данных (`IllegalArgumentException`).

| Ситуация | Пример значения `detail` |
|----------|--------------------------|
| Пустой номер телефона | `Phone must not be blank` |
| Код SMS не отправлялся на этот номер | `No code sent to +79001234567` |
| Код SMS истёк | `Code expired` |
| Код SMS уже был использован | `Code already used` |
| Неверный SMS-код | `Invalid code` |
| Телефон уже зарегистрирован | `Phone already registered: +79001234567` |
| Email уже зарегистрирован | `User email already exists: user@example.com` |
| Заказ не содержит мест или билетов | `Order request must contain seats or admission items` |
| Продажа билетов на событие закрыта | `Ticket sales are closed for event: <eventId>` |
| Код категории уже существует | `Category code already exists: <code>` |
| Код организации уже существует | `Organization code already exists: <code>` |
| Заявка с таким кодом организации уже есть | `Pending organization application already exists for code: <code>` |
| Организация уже владеет этой площадкой | `Organization already owns this venue` |
| Заявка на доступ к площадке уже существует | `A pending or approved request already exists for venue <id> and organization <id>` |
| VenueSpace не принадлежит указанной площадке | `VenueSpace <id> does not belong to venue <id>` |
| Неверный параметр пагинации `page` | `page must be non-negative` |
| Неверный параметр пагинации `size` | `size must be between 1 and 100` |
| Неверный параметр `limit` в ops-эндпоинтах | `limit must be between 1 and 1000` |
| Тип инвентаря не соответствует запросу | `Ticket types are only available for general admission events` |
| Схема зала недоступна для не-seated события | `Seat map is only available for seated events` |
| Ревьюер заявки не является администратором | `Reviewer must be admin: <userId>` |
| Событие ещё не началось | `Event has not started yet: <eventId>` |

---

### 401 Unauthorized

Возникает при проблемах с аутентификацией (`UnauthorizedException`).

| Ситуация | Пример значения `detail` |
|----------|--------------------------|
| Bearer-токен отсутствует | `Missing authorization: provide Bearer token` |
| Токен недействителен или не найден | `Invalid or expired token` |
| Токен истёк | `Token has expired` |

---

### 403 Forbidden

Возникает при нарушении прав доступа (`SecurityException`).

| Ситуация | Пример значения `detail` |
|----------|--------------------------|
| Пользователь не является членом организации события | `User <userId> is not a member of organization <orgId>` |
| Попытка получить заказ/билет другого пользователя | `Access denied` |
| Попытка одобрить/отклонить заявку на площадку без прав владельца | `User <userId> is not a member of organization <orgId>` |

---

### 404 Not Found

Возникает когда сущность не найдена (`NoSuchElementException`).

| Ситуация | Пример значения `detail` |
|----------|--------------------------|
| Событие не найдено | `Event not found: <eventId>` |
| Площадка (venue) не найдена | `Venue not found: <venueId>` |
| Заказ не найден | `Order not found: <orderId>` |
| Инвентарный план события не найден | `EventInventoryPlan not found for event: <eventId>` |
| Заявка на организацию не найдена | `OrganizationApplication not found: <applicationId>` |
| Запрос на доступ к площадке не найден | `Access request not found: <grantId>` |
| LayoutTemplate не найден | `LayoutTemplate not found: <templateId>` |
| VenueSpace не найдена | `VenueSpace not found: <spaceId>` |
| Аккаунт с этим номером не найден (при логине) | `No account found for phone <phone>. Please register first.` |

---

### 409 Conflict

Возникает при конфликте состояния (`IllegalStateException`), а также при сканировании уже использованного билета.

| Ситуация | Пример значения `detail` |
|----------|--------------------------|
| Заказ уже оплачен | `Order is already paid: <orderId>` |
| Заказ уже истёк | `Order is already expired: <orderId>` |
| Оплата заказа уже завершилась неудачей | `Order payment is already failed: <orderId>` |
| Окно оплаты истекло | `Order payment window expired: <orderId>` |
| Попытка оплаты уже провалилась | `Payment attempt already failed for order: <orderId>` |
| Инвентарный план уже существует для события | `EventInventoryPlan already exists for event: <eventId>` |

> **Эндпоинт сканирования билета** (`POST /api/v1/events/{eventId}/tickets/{ticketId}/validate`)
> возвращает `409` с собственным форматом тела (см. [Специальный формат ответа валидации билета](#специальный-формат-ответа-валидации-билета)).

---

### 422 Unprocessable Entity

Используется только эндпоинтом валидации билета, когда билет принадлежит другому событию.

> Возвращается с собственным форматом тела (см. [Специальный формат ответа валидации билета](#специальный-формат-ответа-валидации-билета)).

---

### 429 Too Many Requests

Возникает при превышении rate limit (`TooManyRequestsException`).

| Ситуация | Пример значения `detail` |
|----------|--------------------------|
| Слишком много попыток отправки SMS на один номер | Сообщение из реализации `SmsRateLimiter` |
| Слишком много запросов с невалидным Bearer-токеном с одного IP | `Too many invalid token attempts` |

Лимиты для SMS (из `SendSmsCodeUseCase`):
- Минимальный интервал между отправками: **60 секунд**
- Максимум **5 отправок** в скользящем окне **1 час**
- Срок жизни кода: **5 минут** (300 секунд)

---

### 500 Internal Server Error

Не обрабатывается `ApiExceptionHandler` явно. Spring Boot возвращает стандартный `ProblemDetail` для необработанных исключений, в том числе:

- Ошибки платёжного шлюза (`paymentGateway.createPayment` бросает исключение)
- Нарушения инварианта состояния домена (броски из методов `EventInventoryPlan`, например `holdSeats`, `sellSeats`, `holdAdmission`, `sellAdmission`)
- Прочие неожиданные RuntimeException

---

## Специальный формат ответа валидации билета

Эндпоинт `POST /api/v1/events/{eventId}/tickets/{ticketId}/validate` **не использует** `ProblemDetail`.
Он возвращает собственный объект `TicketValidationResponse` с HTTP-статусом, зависящим от результата.

```json
{
  "status": "VALID | ALREADY_USED | WRONG_EVENT | NOT_FOUND | UNAUTHORIZED",
  "ticketId": "uuid",
  "eventId": "uuid",
  "eventLabel": "string",
  "ticketEventLabel": "string",
  "holderName": "string",
  "seatInfo": "string",
  "price": 100,
  "issuedAt": "2025-01-01T10:00:00Z",
  "usedAt": "2025-01-01T20:00:00Z"
}
```

| Значение поля `status` | HTTP-статус | Описание | Заполненные поля |
|------------------------|-------------|----------|-----------------|
| `VALID` | 200 OK | Билет действителен, помечен как использованный | `ticketId`, `eventId`, `eventLabel`, `holderName`, `seatInfo`, `price`, `issuedAt`, `usedAt` |
| `ALREADY_USED` | 409 Conflict | Билет уже был использован ранее | `ticketId`, `eventLabel`, `holderName`, `usedAt` |
| `WRONG_EVENT` | 422 Unprocessable Entity | Билет принадлежит другому событию | `ticketId`, `eventLabel` (сканируемое), `ticketEventLabel` (реальное) |
| `NOT_FOUND` | 404 Not Found | Билет или событие не найдены | — |
| `UNAUTHORIZED` | 403 Forbidden | Сотрудник не является членом организации события | — |

---

## Domain-специфичные коды (sealed class результатов)

### TicketValidationResult — результаты валидации QR-билета

Определён в `ValidateTicketUseCase`. Не транслируется в `ProblemDetail` — имеет собственную HTTP-маппинг в `TicketController`.

| Вариант | HTTP-статус | Описание |
|---------|-------------|----------|
| `TicketValidationResult.Valid` | 200 | Билет прошёл валидацию, вход разрешён |
| `TicketValidationResult.AlreadyUsed` | 409 | Билет уже был использован |
| `TicketValidationResult.WrongEvent` | 422 | Билет выдан на другое событие |
| `TicketValidationResult.NotFound` | 404 | Билет или событие не найдены |
| `TicketValidationResult.Unauthorized` | 403 | Нет доступа: пользователь не член организации |

### Исключения, транслируемые в HTTP-статусы

| Тип исключения | HTTP-статус | Обработчик в `ApiExceptionHandler` |
|----------------|-------------|-------------------------------------|
| `IllegalArgumentException` | 400 Bad Request | `handleIllegalArgument` |
| `IllegalStateException` | 409 Conflict | `handleIllegalState` |
| `NoSuchElementException` | 404 Not Found | `handleNotFound` |
| `SecurityException` | 403 Forbidden | `handleForbidden` |
| `UnauthorizedException` | 401 Unauthorized | `handleUnauthorized` |
| `TooManyRequestsException` | 429 Too Many Requests | `handleTooManyRequests` |

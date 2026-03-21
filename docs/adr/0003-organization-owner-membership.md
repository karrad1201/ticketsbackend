# ADR 0003: Create Owner Membership on Organization Approval

## Status

Accepted

## Context

После ввода `Organization` и `OrganizationApplication` возникла проблема модели:

- после аппрува организация существует;
- но в системе не зафиксировано, кто именно ею владеет.

Без membership нельзя дальше корректно привязывать права на:

- создание площадок;
- создание мероприятий;
- управление организацией.

## Decision

Вводим `OrganizationMember` c ролями:

- `OWNER`
- `MANAGER`

И фиксируем правило:

- при `approve` заявки на организацию автоматически создается `OrganizationMember`
  для `applicantUserId` с ролью `OWNER`.

## Consequences

Плюсы:

- организация больше не существует без владельца;
- следующий шаг с авторизацией на `Venue` и `Event` можно опирать на membership;
- ownership появляется из бизнес-флоу, а не через отдельную ручную операцию.

Минусы:

- сейчас membership создается внутри того же use case, что и `approve`;
- при переходе на production persistence это потребует транзакционной границы.

## Next Step

Следом нужно:

1. привязать `Venue` к `Organization`;
2. проверять membership/role при создании каталоговых сущностей;
3. затем привязать `Event` к `Organization`.

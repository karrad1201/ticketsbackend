# ADR 0001: Introduce Organization as a Standalone Aggregate First

## Status

Accepted

## Context

По продуктового флоу организация появляется раньше площадок и мероприятий:

- пользователь подает заявку на организацию;
- админ аппрувит заявку;
- после этого организация начинает создавать venue, space и event.

При этом в текущем коде еще нет:

- `User`
- `Admin`
- `OrganizationApplication`
- membership/roles

Если пытаться вводить всю цепочку сразу, первый шаг получится слишком широким и нарушит текущий флоу разработки из `README.md`.

## Decision

Сначала вводим `Organization` как самостоятельную доменную сущность с минимальным CRUD-каркасом:

- `Organization`
- `OrganizationRepository`
- `OrganizationService`
- `CreateOrganizationUseCase`
- `OrganizationController`

На этом шаге `Organization` не связывается:

- с пользователями;
- с заявками;
- с venue;
- с event.

## Consequences

Плюсы:

- появляется базовая доменная опора для дальнейшего флоу;
- сохраняется последовательность `domain -> repository -> service -> tests -> use case -> controller`;
- следующий шаг можно делать уже вокруг `OrganizationApplication`.

Минусы:

- организация пока существует без владельца и без процесса аппрува;
- потребуются последующие миграции доменной модели, когда добавим user/admin/application.

## Next Step

Следом нужно вводить:

1. `User`
2. `OrganizationApplication`
3. approval use case со стороны `Admin`
4. membership пользователя в организации

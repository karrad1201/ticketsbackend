# Current Branch Summary

## Branch

- Branch: `feature/current-state-and-flow-hardening`
- Base: `main`

## Included Work

Ключевые изменения на этой ветке:

- `5306db7` `Handle expired payments, close event sales, and drop tracked target`
- `e4f556e` `Document current state and harden event sales flow`
- `a3d319d` `Add ops automation and query-backed event discovery`

Смысл этих изменений:

- `payment expired` теперь освобождает inventory и не оставляет висящие hold;
- у `Event` есть полноценный flow закрытия продаж;
- закрытие продаж события завершает pending purchase attempts по событию;
- approval организации идет внутри transaction boundary;
- stale pending payments можно пакетно обрабатывать через отдельный use case;
- для started events и stale payments появился scheduler и ручной ops HTTP surface;
- `search` и `discovery` больше не зависят от `findAll()+filter` в application use case.

## Current State

Система сейчас находится в таком состоянии:

- основной runtime: `JDBC + Flyway + H2`;
- dev contour: `in-memory`;
- purchase flow durable и покрыт transaction/use-case path;
- event sales lifecycle теперь включает open -> close on start/manual close -> cleanup pending orders;
- operations flow существует и для автоматического запуска, и для ручного admin trigger;
- `search` и `discovery` используют repository-level query path.

## What Remains

После этого среза остаются в основном не логические дыры, а следующие архитектурные хвосты:

- нет полноценного auth/authz slice, остается временный `X-User-Id` boundary;
- нет отдельной read-model/projection архитектуры для discovery/search, только query-backed repository path;
- нет отдельной job orchestration модели, retries и execution audit для background operations;
- purchase invariants все еще предполагают, что запись в `Event` идет через application flow, а не через произвольные прямые mutation path;
- нет real PSP integration, только mock payment flow и callback model.

## Next Step

Следующий разумный порядок работ:

1. ввести полноценный auth/authz слой вместо временного header-based current user;
2. вынести search/discovery в отдельный read-model/projection slice;
3. добавить richer operations model:
   - execution audit;
   - retry semantics;
   - explicit job state;
4. при необходимости подготовить payment boundary к реальному provider-specific webhook flow.

## Verification

- `make test`
- результат на момент фиксации: `229` tests, `0` failures

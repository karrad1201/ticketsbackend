# ADR 0010: Move Event Discovery And Search To Query-Backed Repository Paths

## Status

Accepted

## Context

Discovery и search уже имели нужную бизнес-логику, но application use case продолжали строить выборки через `findAll()` и фильтрацию в памяти.

Это создавало несколько проблем:

- логика поиска была привязана к полному сканированию репозитория;
- JDBC runtime не использовал возможности БД для предикатов по времени, городу и фильтрам;
- search/discovery оставались слишком дорогими по мере роста каталога;
- application layer содержал лишнюю query-specific фильтрацию.

## Decision

Вводим repository-level query path для событий:

- `findAvailableByCity(city, now)` для discovery;
- `searchAvailable(criteria)` для поиска;
- `findIdsWithStartedOpenSales(now, limit)` для operational auto-close started events.

Для JDBC runtime эти методы реализуются SQL-запросами в `JdbcEventRepository`.

Для `in-memory` runtime сохраняется функциональный эквивалент через локальную фильтрацию, но уже за repository boundary, а не в use case.

Application use case теперь:

- используют `EventAvailabilityService` как единый semantic boundary доступности события;
- больше не строят search/discovery из `findAll()+filter`.

## Consequences

Плюсы:

- query semantics становятся частью repository layer;
- JDBC contour начинает использовать БД для реальных event-selection запросов;
- search и discovery остаются согласованными по правилам доступности события;
- application use case становятся проще и ближе к orchestration, а не к ad-hoc querying.

Минусы:

- это еще не полноценный projection/read-model slice;
- repository contract становится богаче и ближе к query-specific API;
- in-memory runtime все еще остается упрощенной моделью, только с тем же интерфейсом.

## Next Step

1. при росте функциональности вынести discovery/search в отдельные projection/read models;
2. добавить ranking/sorting semantics как отдельную query capability, а не только use case post-processing;
3. при необходимости подготовить SQL indexes и dedicated search storage под production load.

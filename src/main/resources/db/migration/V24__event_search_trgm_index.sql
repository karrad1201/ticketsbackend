-- Issue #205: pg_trgm GIN-индексы для LIKE '%query%' поиска событий
--
-- Проблема: searchAvailable() использует lower(e.label) LIKE ? без индекса,
-- что приводит к seq scan на таблице events.
--
-- Решение: расширение pg_trgm + GIN-индексы позволяют PostgreSQL использовать
-- индекс для LIKE '%query%' / ILIKE '%query%' запросов.
--
-- H2-совместимость: в тестах Flyway отключён (FlywayAutoConfiguration excluded
-- в src/test/resources/application.yml). Jdbc-тесты используют Testcontainers
-- с PostgreSQL 16, где pg_trgm доступен. Миграция безопасна для обоих случаев.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- GIN-индекс для LIKE '%query%' по label событий
-- Покрывает: lower(e.label) LIKE ? в searchAvailable()
CREATE INDEX IF NOT EXISTS idx_events_label_trgm ON events USING gin (label gin_trgm_ops);

-- GIN-индекс для LIKE '%query%' по description событий
-- На случай расширения поиска на description в будущем
CREATE INDEX IF NOT EXISTS idx_events_description_trgm ON events USING gin (description gin_trgm_ops);

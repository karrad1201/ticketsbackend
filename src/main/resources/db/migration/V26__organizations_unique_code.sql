-- #240: UNIQUE constraint на organizations.code
-- Предотвращает создание двух организаций с одинаковым кодом при race condition.
ALTER TABLE organizations ADD CONSTRAINT organizations_code_unique UNIQUE (code);

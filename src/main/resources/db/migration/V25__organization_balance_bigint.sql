-- #236: Organization.balance меняется с INT на BIGINT
-- INT (max ~2.1 млрд копеек = ~21.4 млн руб) недостаточен для высокооборотных организаций.
-- BIGINT (~9.2 * 10^18) устраняет риск переполнения.
ALTER TABLE organizations ALTER COLUMN balance TYPE BIGINT;

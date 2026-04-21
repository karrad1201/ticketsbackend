-- #199: хранить SHA-256 хеш OTP-кода вместо plain-text
-- SHA-256 hex-дайджест занимает 64 символа
alter table sms_codes alter column code type varchar(64);

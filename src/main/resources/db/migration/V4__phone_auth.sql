-- Make email nullable (phone is now the primary auth identifier)
alter table users alter column email drop not null;

-- Add phone field
alter table users add column if not exists phone varchar(32) null;
alter table users add constraint users_phone_unique unique (phone);

-- SMS verification codes
create table if not exists sms_codes (
    id uuid primary key,
    phone varchar(32) not null,
    code varchar(6) not null,
    expires_at timestamp not null,
    used boolean not null default false
);

-- Auth tokens (opaque bearer tokens)
create table if not exists auth_tokens (
    id uuid primary key,
    token varchar(255) not null unique,
    user_id uuid not null references users(id),
    created_at timestamp not null
);

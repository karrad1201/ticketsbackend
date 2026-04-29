create table if not exists admin_credentials (
    id          uuid primary key default gen_random_uuid(),
    user_id     uuid not null references users(id) on delete cascade,
    password_hash varchar(255) not null,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now(),
    constraint admin_credentials_user_id_unique unique (user_id)
);

create table push_tokens (
    id          uuid        primary key,
    user_id     uuid        not null references users(id) on delete cascade,
    token       text        not null unique,
    platform    varchar(10) not null check (platform in ('android', 'ios')),
    created_at  timestamptz not null default now()
);

create index idx_push_tokens_user_id on push_tokens(user_id);

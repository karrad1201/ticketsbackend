alter table auth_tokens
    add column if not exists expires_at timestamptz not null default now() + interval '90 days';

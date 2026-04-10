alter table users add column if not exists avatar_url text null;
alter table users add column if not exists interests text not null default '[]';

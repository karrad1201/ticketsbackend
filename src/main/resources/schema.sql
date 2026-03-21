create table if not exists categories (
    id uuid primary key,
    code varchar(128) not null unique,
    label varchar(255) not null
);

alter table orders
    add column if not exists failed_at timestamp;

create table if not exists payment_attempts (
    id uuid primary key,
    order_id uuid not null,
    reference varchar(255) not null unique,
    amount integer not null,
    status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    confirmed_at timestamp null,
    failure_reason varchar(1024) null
);

create index if not exists idx_payment_attempts_order_id on payment_attempts(order_id);

create table if not exists payment_callback_audits (
    id uuid primary key,
    payment_reference varchar(255) not null,
    status varchar(32) not null,
    received_at timestamp not null,
    payload clob null
);

create index if not exists idx_payment_callback_audits_reference
    on payment_callback_audits(payment_reference);

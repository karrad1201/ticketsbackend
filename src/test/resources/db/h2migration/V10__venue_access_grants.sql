create table if not exists venue_access_grants (
    id               uuid         primary key,
    venue_id         uuid         not null,
    requesting_org_id uuid        not null,
    status           varchar(16)  not null default 'PENDING',
    created_at       timestamp    not null,
    decided_at       timestamp    null,
    decided_by       uuid         null
);

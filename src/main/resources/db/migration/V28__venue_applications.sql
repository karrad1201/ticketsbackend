create table if not exists venue_applications (
    id                  uuid primary key,
    organization_id     uuid not null,
    applicant_user_id   uuid not null,
    name                varchar(255) not null,
    city_label          varchar(255) not null,
    subject_label       varchar(255) not null,
    address             varchar(255) not null,
    description         text null,
    document_urls       text not null default '[]',
    status              varchar(64) not null,
    reviewed_by_user_id uuid null,
    reviewed_at         timestamptz null,
    venue_id            uuid null,
    created_at          timestamptz not null
);

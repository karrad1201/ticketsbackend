create table event_photos (
    id          uuid primary key,
    event_id    uuid not null references events(id) on delete cascade,
    url         text not null,
    sort_order  int  not null default 0,
    uploaded_at timestamptz not null default now()
);

create index idx_event_photos_event_id on event_photos(event_id);

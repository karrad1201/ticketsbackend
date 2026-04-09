create table if not exists favorite_events (
    id         uuid      primary key,
    user_id    uuid      not null references users(id),
    event_id   uuid      not null references events(id),
    created_at timestamp not null,
    unique (user_id, event_id)
);

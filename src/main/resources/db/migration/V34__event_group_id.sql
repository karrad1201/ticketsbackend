alter table events add column group_id uuid null;

create index events_group_id_idx on events (group_id) where group_id is not null;

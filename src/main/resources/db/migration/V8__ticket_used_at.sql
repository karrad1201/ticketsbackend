-- Track when a ticket was scanned/validated by an organizer
alter table tickets add column if not exists used_at timestamp null;

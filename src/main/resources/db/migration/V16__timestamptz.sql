-- Migrate all timestamp columns to timestamptz (timestamp with time zone)
-- Ensures correct timezone handling when running in PostgreSQL

alter table organization_applications
    alter column reviewed_at type timestamptz using reviewed_at at time zone 'UTC';

alter table events
    alter column event_time type timestamptz using event_time at time zone 'UTC',
    alter column sales_closed_at type timestamptz using sales_closed_at at time zone 'UTC';

alter table orders
    alter column expires_at type timestamptz using expires_at at time zone 'UTC',
    alter column created_at type timestamptz using created_at at time zone 'UTC',
    alter column paid_at type timestamptz using paid_at at time zone 'UTC',
    alter column failed_at type timestamptz using failed_at at time zone 'UTC';

alter table tickets
    alter column issued_at type timestamptz using issued_at at time zone 'UTC',
    alter column used_at type timestamptz using used_at at time zone 'UTC';

alter table user_event_visits
    alter column visited_at type timestamptz using visited_at at time zone 'UTC';

alter table event_seat_inventory
    alter column hold_expires_at type timestamptz using hold_expires_at at time zone 'UTC';

alter table payment_attempts
    alter column created_at type timestamptz using created_at at time zone 'UTC',
    alter column updated_at type timestamptz using updated_at at time zone 'UTC',
    alter column confirmed_at type timestamptz using confirmed_at at time zone 'UTC';

alter table payment_callback_audits
    alter column received_at type timestamptz using received_at at time zone 'UTC';

alter table sms_codes
    alter column expires_at type timestamptz using expires_at at time zone 'UTC';

alter table auth_tokens
    alter column created_at type timestamptz using created_at at time zone 'UTC';

alter table venue_access_grants
    alter column created_at type timestamptz using created_at at time zone 'UTC',
    alter column decided_at type timestamptz using decided_at at time zone 'UTC';

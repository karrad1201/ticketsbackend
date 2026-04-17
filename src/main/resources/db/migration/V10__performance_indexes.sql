-- Performance indexes identified in production readiness audit (#171)

-- Speed up event discovery queries filtered by organization
create index if not exists idx_events_organization_id on events(organization_id);

-- Speed up payment reconciliation and status queries on payment_attempts
create index if not exists idx_payment_attempts_status on payment_attempts(status);

-- Speed up order lookups by event (e.g. findPendingByEventId)
create index if not exists idx_orders_event_id on orders(event_id);

-- Speed up user lookups by phone (login flow)
create index if not exists idx_users_phone on users(phone) where phone is not null;

-- Speed up ticket lookups by order and event
create index if not exists idx_tickets_order_id on tickets(order_id);
create index if not exists idx_tickets_event_id on tickets(event_id);

-- Speed up inventory seat lookups
create index if not exists idx_event_seat_inventory_event_id on event_seat_inventory(event_id);
create index if not exists idx_event_admission_inventory_event_id on event_admission_inventory(event_id);

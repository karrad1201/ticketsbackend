-- Tickets: lookups by order, event and user
create index if not exists idx_tickets_order_id on tickets(order_id);
create index if not exists idx_tickets_event_id on tickets(event_id);
create index if not exists idx_tickets_user_id  on tickets(user_id);

-- Orders: scheduler scans for expired pending orders
create index if not exists idx_orders_status_expires_at on orders(status, expires_at)
    where status = 'PENDING_PAYMENT';

-- Event seat inventory: seat availability queries
create index if not exists idx_event_seat_inventory_event_status
    on event_seat_inventory(event_id, status);

-- User event visits: discovery queries by user
create index if not exists idx_user_event_visits_user_id on user_event_visits(user_id);

-- Favorite events: lookups by user
create index if not exists idx_favorite_events_user_id on favorite_events(user_id);

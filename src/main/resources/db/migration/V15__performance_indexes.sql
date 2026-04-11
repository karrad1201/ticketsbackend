-- Performance indexes to speed up hot query paths

-- Phone-based auth lookups (send-code, login, register)
create index if not exists idx_users_phone on users(phone);

-- Order history per buyer (GET /orders?userId=...)
create index if not exists idx_orders_buyer_user_id on orders(buyer_user_id);

-- Order lookups per event (hold/release flows, sales closure)
create index if not exists idx_orders_event_id on orders(event_id);

-- Seat availability checks: find seats held by a specific order
create index if not exists idx_event_seat_inventory_hold_order_id on event_seat_inventory(hold_order_id);

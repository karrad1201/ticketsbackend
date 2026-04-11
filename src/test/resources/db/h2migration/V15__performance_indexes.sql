-- Performance indexes (H2 test mirror)

create index if not exists idx_users_phone on users(phone);
create index if not exists idx_orders_buyer_user_id on orders(buyer_user_id);
create index if not exists idx_orders_event_id on orders(event_id);
create index if not exists idx_event_seat_inventory_hold_order_id on event_seat_inventory(hold_order_id);

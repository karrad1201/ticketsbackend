-- H2-compatible: TYPE without USING clause, correct tables (event_seat_inventory instead of event_inventory_plans)
alter table order_seat_items alter column seat_number type varchar(32);
alter table tickets alter column seat_number type varchar(32);
alter table event_seat_inventory alter column seat_number type varchar(32);

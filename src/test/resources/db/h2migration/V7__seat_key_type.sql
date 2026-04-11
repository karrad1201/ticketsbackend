-- H2-compatible: USING clause omitted (H2 does not support PostgreSQL cast syntax)
alter table event_seat_inventory alter column seat_number type varchar(32);
alter table order_seat_items alter column seat_number type varchar(32);
alter table tickets alter column seat_number type varchar(32);

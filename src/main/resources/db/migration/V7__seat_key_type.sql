alter table event_seat_inventory alter column seat_number type varchar(32) using seat_number::varchar;
alter table order_seat_items alter column seat_number type varchar(32) using seat_number::varchar;
alter table tickets alter column seat_number type varchar(32) using seat_number::varchar;

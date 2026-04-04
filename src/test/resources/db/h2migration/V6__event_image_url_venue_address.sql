alter table events add column if not exists image_url varchar(2048) null;
alter table events add column if not exists min_price integer null;
alter table venues add column if not exists address varchar(512) null;

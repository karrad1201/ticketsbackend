alter table events
    add column if not exists sales_closed_at timestamp null;

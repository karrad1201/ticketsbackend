create table if not exists users (
    id uuid primary key,
    email varchar(255) not null unique,
    full_name varchar(255) not null,
    role varchar(32) not null
);

create table if not exists organizations (
    id uuid primary key,
    code varchar(255) not null unique,
    name varchar(255) not null,
    balance integer not null
);

create table if not exists categories (
    id uuid primary key,
    code varchar(255) not null unique,
    label varchar(255) not null
);

create table if not exists organization_members (
    id uuid primary key,
    organization_id uuid not null,
    user_id uuid not null,
    role varchar(64) not null
);

create table if not exists organization_applications (
    id uuid primary key,
    applicant_user_id uuid not null,
    organization_code varchar(255) not null,
    organization_name varchar(255) not null,
    status varchar(64) not null,
    reviewed_by_user_id uuid null,
    reviewed_at timestamp null,
    organization_id uuid null
);

create table if not exists venues (
    id uuid primary key,
    label varchar(255) not null,
    city_label varchar(255) not null,
    subject_label varchar(255) not null,
    organization_id uuid null
);

create table if not exists venue_spaces (
    id uuid primary key,
    venue_id uuid not null,
    label varchar(255) not null
);

create table if not exists layout_templates (
    id uuid primary key,
    venue_space_id uuid not null,
    label varchar(255) not null
);

create table if not exists layout_template_sections (
    layout_template_id uuid not null,
    section_key varchar(255) not null,
    label varchar(255) not null,
    sort_order integer not null,
    primary key (layout_template_id, section_key)
);

create table if not exists layout_template_rows (
    layout_template_id uuid not null,
    section_key varchar(255) not null,
    row_key varchar(255) not null,
    label varchar(255) not null,
    start_seat integer not null,
    end_seat integer not null,
    price integer not null,
    sort_order integer not null,
    primary key (layout_template_id, section_key, row_key)
);

create table if not exists events (
    id uuid primary key,
    label varchar(255) not null,
    description varchar(1024) not null,
    venue_id uuid not null,
    category_id uuid not null,
    event_time timestamp not null,
    venue_space_id uuid null,
    organization_id uuid null
);

create table if not exists event_inventory_plans (
    event_id uuid primary key,
    mode varchar(64) not null,
    layout_template_id uuid null
);

create table if not exists orders (
    id uuid primary key,
    event_id uuid not null,
    buyer_user_id uuid not null,
    amount integer not null,
    expires_at timestamp not null,
    payment_reference varchar(255) not null,
    payment_url varchar(1024) not null,
    status varchar(64) not null,
    created_at timestamp not null,
    paid_at timestamp null
);

create table if not exists order_seat_items (
    order_id uuid not null,
    section_key varchar(255) not null,
    row_key varchar(255) not null,
    seat_number integer not null,
    primary key (order_id, section_key, row_key, seat_number)
);

create table if not exists order_admission_items (
    order_id uuid not null,
    ticket_type_id uuid not null,
    quantity integer not null,
    primary key (order_id, ticket_type_id)
);

create table if not exists tickets (
    id uuid primary key,
    order_id uuid not null,
    event_id uuid not null,
    user_id uuid not null,
    price integer not null,
    section_key varchar(255) null,
    row_key varchar(255) null,
    seat_number integer null,
    ticket_type_id uuid null,
    issued_at timestamp not null
);

create table if not exists event_seat_inventory (
    event_id uuid not null,
    section_key varchar(255) not null,
    row_key varchar(255) not null,
    seat_number integer not null,
    price integer not null,
    status varchar(32) not null,
    hold_order_id uuid null,
    hold_expires_at timestamp null,
    primary key (event_id, section_key, row_key, seat_number)
);

create table if not exists event_admission_inventory (
    event_id uuid not null,
    ticket_type_id uuid not null,
    price integer not null,
    capacity integer not null,
    held integer not null,
    sold integer not null,
    primary key (event_id, ticket_type_id)
);

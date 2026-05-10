create table space_price_profiles (
    id uuid primary key,
    venue_space_id uuid not null,
    label varchar(255) not null,
    mode varchar(50) not null
);

create index space_price_profiles_venue_space_id_idx on space_price_profiles (venue_space_id);

create table space_price_profile_sections (
    profile_id uuid not null references space_price_profiles (id),
    section_key varchar(255) not null,
    price int not null,
    sort_order int not null default 0
);

create table space_price_profile_ticket_types (
    profile_id uuid not null references space_price_profiles (id),
    label varchar(255) not null,
    price int not null,
    quota int not null,
    sort_order int not null default 0
);

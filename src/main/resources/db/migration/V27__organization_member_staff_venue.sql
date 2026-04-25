alter table organization_members
    add column venue_id uuid references venues(id);

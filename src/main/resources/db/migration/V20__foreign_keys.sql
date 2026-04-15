-- Orders → events / users
alter table orders add constraint fk_orders_event_id
    foreign key (event_id) references events(id);

alter table orders add constraint fk_orders_buyer_user_id
    foreign key (buyer_user_id) references users(id);

-- Order items → orders (cascade: удаление заказа удаляет позиции)
alter table order_seat_items add constraint fk_order_seat_items_order_id
    foreign key (order_id) references orders(id) on delete cascade;

alter table order_admission_items add constraint fk_order_admission_items_order_id
    foreign key (order_id) references orders(id) on delete cascade;

-- Payment attempts → orders (restrict: нельзя удалить заказ с платежами)
alter table payment_attempts add constraint fk_payment_attempts_order_id
    foreign key (order_id) references orders(id);

-- Tickets → orders / events / users
alter table tickets add constraint fk_tickets_order_id
    foreign key (order_id) references orders(id);

alter table tickets add constraint fk_tickets_event_id
    foreign key (event_id) references events(id);

alter table tickets add constraint fk_tickets_user_id
    foreign key (user_id) references users(id);

-- Venue spaces → venues (cascade: удаление зала удаляет пространства)
alter table venue_spaces add constraint fk_venue_spaces_venue_id
    foreign key (venue_id) references venues(id) on delete cascade;

-- Layout templates → venue spaces (cascade: без пространства шаблон бесполезен)
alter table layout_templates add constraint fk_layout_templates_venue_space_id
    foreign key (venue_space_id) references venue_spaces(id) on delete cascade;

-- Favorite events → events (cascade: удаление события очищает избранное)
alter table favorite_events add constraint fk_favorite_events_event_id
    foreign key (event_id) references events(id) on delete cascade;

-- Organization applications → users
alter table organization_applications add constraint fk_org_applications_applicant_user_id
    foreign key (applicant_user_id) references users(id);

-- Organization members → users / organizations (cascade: удаление пользователя или орг. удаляет членство)
alter table organization_members add constraint fk_org_members_user_id
    foreign key (user_id) references users(id) on delete cascade;

alter table organization_members add constraint fk_org_members_organization_id
    foreign key (organization_id) references organizations(id) on delete cascade;

-- Venue access grants → venues / organizations (cascade)
alter table venue_access_grants add constraint fk_venue_access_grants_venue_id
    foreign key (venue_id) references venues(id) on delete cascade;

alter table venue_access_grants add constraint fk_venue_access_grants_requesting_org_id
    foreign key (requesting_org_id) references organizations(id) on delete cascade;

-- Auth tokens / sms codes → users (cascade: удаление пользователя удаляет его токены и коды)
alter table auth_tokens add constraint fk_auth_tokens_user_id
    foreign key (user_id) references users(id) on delete cascade;

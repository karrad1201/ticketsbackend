-- auth_tokens.token — hot path: каждый HTTP-запрос через CurrentUserProvider
create index if not exists idx_auth_tokens_token on auth_tokens(token);

-- auth_tokens.user_id — удаление всех токенов пользователя при logout
create index if not exists idx_auth_tokens_user_id on auth_tokens(user_id);

-- sms_codes.phone — каждая отправка и верификация кода
create index if not exists idx_sms_codes_phone on sms_codes(phone);

-- users.phone — логин / регистрация
create index if not exists idx_users_phone on users(phone);

-- organization_members(organization_id, user_id) — проверка членства (auth checks)
create unique index if not exists idx_org_members_org_user on organization_members(organization_id, user_id);

-- venue_access_grants.venue_id — findByVenueId()
create index if not exists idx_venue_access_grants_venue_id on venue_access_grants(venue_id);

-- venue_spaces.venue_id — JOIN в VenueQueryService
create index if not exists idx_venue_spaces_venue_id on venue_spaces(venue_id);

-- layout_templates.venue_space_id — findByVenueSpaceId() (при промахе Caffeine-кэша)
create index if not exists idx_layout_templates_venue_space_id on layout_templates(venue_space_id);

-- order_seat_items.order_id — DELETE при JdbcOrderRepository.save()
create index if not exists idx_order_seat_items_order_id on order_seat_items(order_id);

-- order_admission_items.order_id — DELETE при JdbcOrderRepository.save()
create index if not exists idx_order_admission_items_order_id on order_admission_items(order_id);

-- orders(event_id, status) — findPendingByEventId() в CloseEventSalesUseCase
create index if not exists idx_orders_event_id_status on orders(event_id, status);

-- event_admission_inventory(event_id, ticket_type_id) — reserveAdmission / confirmAdmission / releaseAdmission
create index if not exists idx_event_admission_inv_event_ticket on event_admission_inventory(event_id, ticket_type_id);

-- favorite_events(user_id, created_at DESC) — ORDER BY в FavoriteQueryService
create index if not exists idx_favorite_events_user_created on favorite_events(user_id, created_at desc);

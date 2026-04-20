-- Performance indexes for event discovery and filtering (issue #206)

-- events.event_time — ORDER BY и WHERE event_time > ? во всех discovery-запросах
create index if not exists idx_events_event_time on events(event_time);

-- events.sales_closed_at — фильтр доступности: WHERE sales_closed_at is null
create index if not exists idx_events_sales_closed_at on events(sales_closed_at);

-- events.category_id — фильтр в searchAvailable(criteria.categoryId)
create index if not exists idx_events_category_id on events(category_id);

-- events.venue_id — фильтр в searchAvailable(criteria.venueId) и findByVenueId()
create index if not exists idx_events_venue_id on events(venue_id);

-- venues.city_label — JOIN-фильтр lower(v.city_label) = ? в findAvailableByCity / searchAvailable
create index if not exists idx_venues_city_label on venues(city_label);

-- Составной индекс для типичного discovery-запроса: доступные события по времени
-- Покрывает: WHERE sales_closed_at IS NULL AND event_time > ? ORDER BY event_time
create index if not exists idx_events_discovery on events(sales_closed_at, event_time)
    where sales_closed_at is null;

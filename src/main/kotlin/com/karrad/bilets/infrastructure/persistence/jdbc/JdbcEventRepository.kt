package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.repository.EventSearchCriteria
import com.karrad.bilets.domain.repository.EventRepository
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.util.UUID

class JdbcEventRepository(
    private val jdbcTemplate: JdbcTemplate
) : EventRepository {

    override fun save(event: Event): Event {
        jdbcTemplate.update(
            """
            insert into events (id, label, description, venue_id, category_id, event_time, venue_space_id, organization_id, sales_closed_at, image_url, min_price, age_rating, has_seat_map)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            on conflict (id) do update set
              label = excluded.label, description = excluded.description, venue_id = excluded.venue_id,
              category_id = excluded.category_id, event_time = excluded.event_time,
              venue_space_id = excluded.venue_space_id, organization_id = excluded.organization_id,
              sales_closed_at = excluded.sales_closed_at, image_url = excluded.image_url,
              min_price = excluded.min_price, age_rating = excluded.age_rating, has_seat_map = excluded.has_seat_map
            """.trimIndent(),
            event.id, event.label, event.description, event.venueId, event.categoryId,
            Timestamp.from(event.time), event.venueSpaceId, event.organizationId,
            instantToTimestamp(event.salesClosedAt), event.imageUrl, event.minPrice,
            event.ageRating, event.hasSeatMap
        )
        return event
    }

    private fun rowMapper(rs: java.sql.ResultSet) = Event(
        label = rs.getString("label"),
        description = rs.getString("description"),
        venueId = rs.uuid("venue_id"),
        categoryId = rs.uuid("category_id"),
        time = rs.instant("event_time"),
        venueSpaceId = rs.nullableUuid("venue_space_id"),
        id = rs.uuid("id"),
        organizationId = rs.nullableUuid("organization_id"),
        salesClosedAt = rs.nullableInstant("sales_closed_at"),
        imageUrl = rs.getString("image_url"),
        minPrice = rs.getObject("min_price") as Int?,
        ageRating = rs.getString("age_rating"),
        hasSeatMap = rs.getBoolean("has_seat_map"),
        venueLabel = rs.getString("venue_label"),
        categoryLabel = rs.getString("category_label")
    )

    private val selectAll = """
        select e.id, e.label, e.description, e.venue_id, e.category_id, e.event_time,
               e.venue_space_id, e.organization_id, e.sales_closed_at, e.image_url,
               e.min_price, e.age_rating, e.has_seat_map,
               v.label as venue_label, c.label as category_label
        from events e
        left join venues v on v.id = e.venue_id
        left join categories c on c.id = e.category_id
    """.trimIndent()

    override fun findById(id: UUID): Event? = jdbcTemplate.query(
        "$selectAll\nwhere id = ?",
        { rs, _ -> rowMapper(rs) },
        id
    ).singleOrNull()

    override fun findAllByIds(ids: Collection<UUID>): List<Event> {
        if (ids.isEmpty()) return emptyList()
        return jdbcTemplate.query(
            { conn ->
                conn.prepareStatement("$selectAll\nwhere id = ANY(?)").apply {
                    setArray(1, conn.createArrayOf("uuid", ids.toTypedArray()))
                }
            },
            { rs, _ -> rowMapper(rs) }
        )
    }

    override fun findAll(): List<Event> = jdbcTemplate.query(
        "$selectAll\norder by event_time, id"
    ) { rs, _ -> rowMapper(rs) }

    override fun findAll(offset: Int, limit: Int): List<Event> = jdbcTemplate.query(
        "$selectAll\norder by event_time, id\nLIMIT ? OFFSET ?",
        { rs, _ -> rowMapper(rs) },
        limit,
        offset
    )

    override fun findByVenueId(venueId: UUID): List<Event> = jdbcTemplate.query(
        "$selectAll\nwhere venue_id = ?\norder by event_time, id",
        { rs, _ -> rowMapper(rs) },
        venueId
    )

    override fun findAvailableByCity(city: String, now: java.time.Instant): List<Event> = jdbcTemplate.query(
        """
        select e.id, e.label, e.description, e.venue_id, e.category_id, e.event_time, e.venue_space_id, e.organization_id, e.sales_closed_at, e.image_url, e.min_price, e.age_rating, e.has_seat_map,
               v.label as venue_label, c.label as category_label
        from events e
        join venues v on v.id = e.venue_id
        left join categories c on c.id = e.category_id
        where lower(v.city_label) = ?
          and e.sales_closed_at is null
          and e.event_time > ?
        order by e.event_time, e.id
        """.trimIndent(),
        { rs, _ -> rowMapper(rs) },
        city.trim().lowercase(),
        Timestamp.from(now)
    )

    override fun findAvailableByCity(city: String, now: java.time.Instant, date: java.time.LocalDate?, limit: Int): List<Event> {
        val params = mutableListOf<Any>(city.trim().lowercase(), Timestamp.from(now))
        val dateFilter = if (date != null) {
            val dayStart = Timestamp.from(date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant())
            val dayEnd = Timestamp.from(date.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant())
            params += dayStart
            params += dayEnd
            "and e.event_time >= ? and e.event_time < ?"
        } else ""
        params += limit
        return jdbcTemplate.query(
            """
            select e.id, e.label, e.description, e.venue_id, e.category_id, e.event_time, e.venue_space_id, e.organization_id, e.sales_closed_at, e.image_url, e.min_price, e.age_rating, e.has_seat_map,
                   v.label as venue_label, c.label as category_label
            from events e
            join venues v on v.id = e.venue_id
            left join categories c on c.id = e.category_id
            where lower(v.city_label) = ?
              and e.sales_closed_at is null
              and e.event_time > ?
              $dateFilter
            order by e.event_time, e.id
            limit ?
            """.trimIndent(),
            { rs, _ -> rowMapper(rs) },
            *params.toTypedArray()
        )
    }

    override fun searchAvailable(criteria: EventSearchCriteria): List<Event> {
        val sql = StringBuilder(
            """
            select e.id, e.label, e.description, e.venue_id, e.category_id, e.event_time, e.venue_space_id, e.organization_id, e.sales_closed_at, e.image_url, e.min_price, e.age_rating, e.has_seat_map,
                   v.label as venue_label, c.label as category_label
            from events e
            join venues v on v.id = e.venue_id
            left join categories c on c.id = e.category_id
            where e.sales_closed_at is null
              and e.event_time > ?
            """.trimIndent()
        )
        val params = mutableListOf<Any>(Timestamp.from(criteria.now))

        val normalizedQuery = criteria.query?.trim()?.lowercase().orEmpty()
        if (normalizedQuery.isNotBlank()) {
            sql.append("\n  and lower(e.label) like ?")
            params += "%$normalizedQuery%"
        }
        val normalizedCity = criteria.city?.trim()?.lowercase()
        if (normalizedCity != null) {
            sql.append("\n  and lower(v.city_label) = ?")
            params += normalizedCity
        }
        if (criteria.categoryId != null) {
            sql.append("\n  and e.category_id = ?")
            params += criteria.categoryId
        }
        if (criteria.venueId != null) {
            sql.append("\n  and e.venue_id = ?")
            params += criteria.venueId
        }
        if (criteria.dateFrom != null) {
            sql.append("\n  and cast(e.event_time as date) >= ?")
            params += java.sql.Date.valueOf(criteria.dateFrom)
        }
        if (criteria.dateTo != null) {
            sql.append("\n  and cast(e.event_time as date) <= ?")
            params += java.sql.Date.valueOf(criteria.dateTo)
        }
        sql.append("\norder by e.event_time, e.id")

        return jdbcTemplate.query(
            sql.toString(),
            { rs, _ -> rowMapper(rs) },
            *params.toTypedArray()
        )
    }

    override fun findUpcomingByOrganizationId(organizationId: UUID, now: java.time.Instant): List<Event> =
        jdbcTemplate.query(
            "$selectAll\nwhere organization_id = ?\n  and event_time > ?\norder by event_time, id",
            { rs, _ -> rowMapper(rs) },
            organizationId,
            Timestamp.from(now)
        )

    override fun findIdsWithStartedOpenSales(now: java.time.Instant, limit: Int): List<UUID> {
        require(limit > 0) { "limit must be positive" }
        return jdbcTemplate.query(
            """
            select id
            from events
            where sales_closed_at is null
              and event_time <= ?
            order by event_time, id
            limit ?
            """.trimIndent(),
            { rs, _ -> rs.uuid("id") },
            Timestamp.from(now),
            limit
        )
    }

    override fun deleteById(id: UUID): Boolean = jdbcTemplate.update(
        "delete from events where id = ?",
        id
    ) > 0
}

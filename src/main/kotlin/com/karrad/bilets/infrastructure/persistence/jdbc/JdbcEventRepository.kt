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
        val updated = jdbcTemplate.update(
            """
            update events
            set label = ?, description = ?, venue_id = ?, category_id = ?, event_time = ?, venue_space_id = ?, organization_id = ?, sales_closed_at = ?, image_url = ?, min_price = ?, age_rating = ?
            where id = ?
            """.trimIndent(),
            event.label,
            event.description,
            event.venueId,
            event.categoryId,
            Timestamp.from(event.time),
            event.venueSpaceId,
            event.organizationId,
            instantToTimestamp(event.salesClosedAt),
            event.imageUrl,
            event.minPrice,
            event.ageRating,
            event.id
        )
        if (updated == 0) {
            jdbcTemplate.update(
                """
                insert into events (id, label, description, venue_id, category_id, event_time, venue_space_id, organization_id, sales_closed_at, image_url, min_price, age_rating)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                event.id,
                event.label,
                event.description,
                event.venueId,
                event.categoryId,
                Timestamp.from(event.time),
                event.venueSpaceId,
                event.organizationId,
                instantToTimestamp(event.salesClosedAt),
                event.imageUrl,
                event.minPrice,
                event.ageRating
            )
        }
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
        ageRating = rs.getString("age_rating")
    )

    private val selectAll = """
        select id, label, description, venue_id, category_id, event_time, venue_space_id, organization_id, sales_closed_at, image_url, min_price, age_rating
        from events
    """.trimIndent()

    override fun findById(id: UUID): Event? = jdbcTemplate.query(
        "$selectAll\nwhere id = ?",
        { rs, _ -> rowMapper(rs) },
        id
    ).singleOrNull()

    override fun findAll(): List<Event> = jdbcTemplate.query(
        "$selectAll\norder by event_time, id"
    ) { rs, _ -> rowMapper(rs) }

    override fun findByVenueId(venueId: UUID): List<Event> = jdbcTemplate.query(
        "$selectAll\nwhere venue_id = ?\norder by event_time, id",
        { rs, _ -> rowMapper(rs) },
        venueId
    )

    override fun findAvailableByCity(city: String, now: java.time.Instant): List<Event> = jdbcTemplate.query(
        """
        select e.id, e.label, e.description, e.venue_id, e.category_id, e.event_time, e.venue_space_id, e.organization_id, e.sales_closed_at, e.image_url, e.min_price, e.age_rating
        from events e
        join venues v on v.id = e.venue_id
        where lower(v.city_label) = ?
          and e.sales_closed_at is null
          and e.event_time > ?
        order by e.event_time, e.id
        """.trimIndent(),
        { rs, _ -> rowMapper(rs) },
        city.trim().lowercase(),
        Timestamp.from(now)
    )

    override fun searchAvailable(criteria: EventSearchCriteria): List<Event> {
        val sql = StringBuilder(
            """
            select e.id, e.label, e.description, e.venue_id, e.category_id, e.event_time, e.venue_space_id, e.organization_id, e.sales_closed_at, e.image_url, e.min_price, e.age_rating
            from events e
            join venues v on v.id = e.venue_id
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

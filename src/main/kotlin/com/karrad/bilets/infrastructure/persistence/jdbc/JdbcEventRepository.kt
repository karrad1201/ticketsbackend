package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.Event
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
            set label = ?, description = ?, venue_id = ?, category_id = ?, event_time = ?, venue_space_id = ?, organization_id = ?, sales_closed_at = ?
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
            event.id
        )
        if (updated == 0) {
            jdbcTemplate.update(
                """
                insert into events (id, label, description, venue_id, category_id, event_time, venue_space_id, organization_id, sales_closed_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                event.id,
                event.label,
                event.description,
                event.venueId,
                event.categoryId,
                Timestamp.from(event.time),
                event.venueSpaceId,
                event.organizationId,
                instantToTimestamp(event.salesClosedAt)
            )
        }
        return event
    }

    override fun findById(id: UUID): Event? = jdbcTemplate.query(
        """
        select id, label, description, venue_id, category_id, event_time, venue_space_id, organization_id, sales_closed_at
        from events
        where id = ?
        """.trimIndent(),
        { rs, _ ->
            Event(
                label = rs.getString("label"),
                description = rs.getString("description"),
                venueId = rs.uuid("venue_id"),
                categoryId = rs.uuid("category_id"),
                time = rs.instant("event_time"),
                venueSpaceId = rs.nullableUuid("venue_space_id"),
                id = rs.uuid("id"),
                organizationId = rs.nullableUuid("organization_id"),
                salesClosedAt = rs.nullableInstant("sales_closed_at")
            )
        },
        id
    ).singleOrNull()

    override fun findAll(): List<Event> = jdbcTemplate.query(
        """
        select id, label, description, venue_id, category_id, event_time, venue_space_id, organization_id, sales_closed_at
        from events
        order by event_time, id
        """.trimIndent()
    ) { rs, _ ->
        Event(
            label = rs.getString("label"),
            description = rs.getString("description"),
            venueId = rs.uuid("venue_id"),
            categoryId = rs.uuid("category_id"),
            time = rs.instant("event_time"),
            venueSpaceId = rs.nullableUuid("venue_space_id"),
            id = rs.uuid("id"),
            organizationId = rs.nullableUuid("organization_id"),
            salesClosedAt = rs.nullableInstant("sales_closed_at")
        )
    }

    override fun findByVenueId(venueId: UUID): List<Event> = jdbcTemplate.query(
        """
        select id, label, description, venue_id, category_id, event_time, venue_space_id, organization_id, sales_closed_at
        from events
        where venue_id = ?
        order by event_time, id
        """.trimIndent(),
        { rs, _ ->
            Event(
                label = rs.getString("label"),
                description = rs.getString("description"),
                venueId = rs.uuid("venue_id"),
                categoryId = rs.uuid("category_id"),
                time = rs.instant("event_time"),
                venueSpaceId = rs.nullableUuid("venue_space_id"),
                id = rs.uuid("id"),
                organizationId = rs.nullableUuid("organization_id"),
                salesClosedAt = rs.nullableInstant("sales_closed_at")
            )
        },
        venueId
    )

    override fun deleteById(id: UUID): Boolean = jdbcTemplate.update(
        "delete from events where id = ?",
        id
    ) > 0
}

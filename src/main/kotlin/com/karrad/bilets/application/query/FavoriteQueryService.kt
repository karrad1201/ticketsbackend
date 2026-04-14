package com.karrad.bilets.application.query

import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.infrastructure.persistence.jdbc.instant
import com.karrad.bilets.infrastructure.persistence.jdbc.nullableInstant
import com.karrad.bilets.infrastructure.persistence.jdbc.nullableUuid
import com.karrad.bilets.infrastructure.persistence.jdbc.uuid
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "jdbc")
class FavoriteQueryService(private val jdbcTemplate: JdbcTemplate) : FavoriteQueryPort {

    override fun listFavoriteEvents(userId: UUID, page: Int, size: Int): List<Event> {
        val offset = page * size
        return jdbcTemplate.query(
            """
            SELECT e.id, e.label, e.description, e.venue_id, e.category_id, e.event_time,
                   e.venue_space_id, e.organization_id, e.sales_closed_at, e.image_url,
                   e.min_price, e.age_rating, e.has_seat_map
            FROM favorite_events f
            JOIN events e ON e.id = f.event_id
            WHERE f.user_id = ?
            ORDER BY f.created_at DESC, e.id
            LIMIT ? OFFSET ?
            """.trimIndent(),
            { rs, _ ->
                Event(
                    id = rs.uuid("id"),
                    label = rs.getString("label"),
                    description = rs.getString("description"),
                    venueId = rs.uuid("venue_id"),
                    categoryId = rs.uuid("category_id"),
                    time = rs.instant("event_time"),
                    venueSpaceId = rs.nullableUuid("venue_space_id"),
                    organizationId = rs.nullableUuid("organization_id"),
                    salesClosedAt = rs.nullableInstant("sales_closed_at"),
                    imageUrl = rs.getString("image_url"),
                    minPrice = rs.getObject("min_price") as Int?,
                    ageRating = rs.getString("age_rating"),
                    hasSeatMap = rs.getBoolean("has_seat_map")
                )
            },
            userId, size, offset
        )
    }
}

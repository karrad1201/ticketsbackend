package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.FavoriteEvent
import com.karrad.bilets.domain.repository.FavoriteEventRepository
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.util.UUID

class JdbcFavoriteEventRepository(
    private val jdbcTemplate: JdbcTemplate
) : FavoriteEventRepository {

    override fun save(favorite: FavoriteEvent): FavoriteEvent {
        jdbcTemplate.update(
            """
            insert into favorite_events (id, user_id, event_id, created_at)
            values (?, ?, ?, ?)
            on conflict (user_id, event_id) do nothing
            """.trimIndent(),
            favorite.id,
            favorite.userId,
            favorite.eventId,
            Timestamp.from(favorite.createdAt)
        )
        return favorite
    }

    override fun findByUserId(userId: UUID): List<FavoriteEvent> =
        jdbcTemplate.query(
            "select id, user_id, event_id, created_at from favorite_events where user_id = ? order by created_at desc",
            { rs, _ ->
                FavoriteEvent(
                    id = rs.uuid("id"),
                    userId = rs.uuid("user_id"),
                    eventId = rs.uuid("event_id"),
                    createdAt = rs.instant("created_at")
                )
            },
            userId
        )

    override fun findByUserIdAndEventId(userId: UUID, eventId: UUID): FavoriteEvent? =
        jdbcTemplate.query(
            "select id, user_id, event_id, created_at from favorite_events where user_id = ? and event_id = ?",
            { rs, _ ->
                FavoriteEvent(
                    id = rs.uuid("id"),
                    userId = rs.uuid("user_id"),
                    eventId = rs.uuid("event_id"),
                    createdAt = rs.instant("created_at")
                )
            },
            userId,
            eventId
        ).singleOrNull()

    override fun deleteByUserIdAndEventId(userId: UUID, eventId: UUID): Boolean =
        jdbcTemplate.update(
            "delete from favorite_events where user_id = ? and event_id = ?",
            userId,
            eventId
        ) > 0
}

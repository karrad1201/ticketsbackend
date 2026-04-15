package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.UserEventVisit
import com.karrad.bilets.domain.repository.UserEventVisitRepository
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

class JdbcUserEventVisitRepository(
    private val jdbcTemplate: JdbcTemplate
) : UserEventVisitRepository {

    override fun save(userEventVisit: UserEventVisit): UserEventVisit {
        val updated = jdbcTemplate.update(
            """
            update user_event_visits
            set user_id = ?, event_id = ?, visited_at = ?
            where id = ?
            """.trimIndent(),
            userEventVisit.userId,
            userEventVisit.eventId,
            instantToTimestamp(userEventVisit.visitedAt),
            userEventVisit.id
        )
        if (updated == 0) {
            jdbcTemplate.update(
                """
                insert into user_event_visits (id, user_id, event_id, visited_at)
                values (?, ?, ?, ?)
                """.trimIndent(),
                userEventVisit.id,
                userEventVisit.userId,
                userEventVisit.eventId,
                instantToTimestamp(userEventVisit.visitedAt)
            )
        }
        return userEventVisit
    }

    override fun findById(id: UUID): UserEventVisit? = jdbcTemplate.query(
        """
        select id, user_id, event_id, visited_at
        from user_event_visits
        where id = ?
        """.trimIndent(),
        { rs, _ -> mapVisit(rs) },
        id
    ).singleOrNull()

    override fun findAll(): List<UserEventVisit> = jdbcTemplate.query(
        """
        select id, user_id, event_id, visited_at
        from user_event_visits
        order by visited_at desc, id
        """.trimIndent()
    ) { rs, _ -> mapVisit(rs) }

    override fun findByUserId(userId: UUID): List<UserEventVisit> = jdbcTemplate.query(
        """
        select id, user_id, event_id, visited_at
        from user_event_visits
        where user_id = ?
        order by visited_at desc, id
        """.trimIndent(),
        { rs, _ -> mapVisit(rs) },
        userId
    )

    override fun findRecentByUserId(userId: UUID, limit: Int): List<UserEventVisit> = jdbcTemplate.query(
        """
        select id, user_id, event_id, visited_at
        from user_event_visits
        where user_id = ?
        order by visited_at desc, id
        limit ?
        """.trimIndent(),
        { rs, _ -> mapVisit(rs) },
        userId,
        limit
    )

    override fun deleteById(id: UUID): Boolean = jdbcTemplate.update(
        "delete from user_event_visits where id = ?",
        id
    ) > 0

    private fun mapVisit(rs: java.sql.ResultSet): UserEventVisit =
        UserEventVisit(
            userId = rs.uuid("user_id"),
            eventId = rs.uuid("event_id"),
            visitedAt = rs.instant("visited_at"),
            id = rs.uuid("id")
        )
}

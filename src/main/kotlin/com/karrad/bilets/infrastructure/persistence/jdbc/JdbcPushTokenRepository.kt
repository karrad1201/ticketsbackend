package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.PushToken
import com.karrad.bilets.domain.repository.PushTokenRepository
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

class JdbcPushTokenRepository(
    private val jdbcTemplate: JdbcTemplate
) : PushTokenRepository {

    override fun save(pushToken: PushToken): PushToken {
        jdbcTemplate.update(
            """
            insert into push_tokens (id, user_id, token, platform, created_at)
            values (?, ?, ?, ?, ?)
            on conflict (token) do update set
              user_id = excluded.user_id,
              platform = excluded.platform,
              created_at = excluded.created_at
            """.trimIndent(),
            pushToken.id,
            pushToken.userId,
            pushToken.token,
            pushToken.platform,
            instantToTimestamp(pushToken.createdAt)
        )
        return pushToken
    }

    override fun findByUserId(userId: UUID): List<PushToken> =
        jdbcTemplate.query(
            "select id, user_id, token, platform, created_at from push_tokens where user_id = ?",
            { rs, _ ->
                PushToken(
                    id = UUID.fromString(rs.getString("id")),
                    userId = UUID.fromString(rs.getString("user_id")),
                    token = rs.getString("token"),
                    platform = rs.getString("platform"),
                    createdAt = rs.getTimestamp("created_at").toInstant()
                )
            },
            userId
        )

    override fun deleteByToken(token: String) {
        jdbcTemplate.update("delete from push_tokens where token = ?", token)
    }
}

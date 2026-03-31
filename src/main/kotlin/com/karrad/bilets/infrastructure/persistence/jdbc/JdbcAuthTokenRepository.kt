package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.AuthToken
import com.karrad.bilets.domain.repository.AuthTokenRepository
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.util.UUID

class JdbcAuthTokenRepository(private val jdbcTemplate: JdbcTemplate) : AuthTokenRepository {

    override fun save(authToken: AuthToken): AuthToken {
        jdbcTemplate.update(
            "insert into auth_tokens (id, token, user_id, created_at) values (?, ?, ?, ?)",
            authToken.id, authToken.token, authToken.userId,
            Timestamp.from(authToken.createdAt)
        )
        return authToken
    }

    override fun findByToken(token: String): AuthToken? = jdbcTemplate.query(
        "select id, token, user_id, created_at from auth_tokens where token = ?",
        { rs, _ ->
            AuthToken(
                id = UUID.fromString(rs.getString("id")),
                token = rs.getString("token"),
                userId = UUID.fromString(rs.getString("user_id")),
                createdAt = rs.getTimestamp("created_at").toInstant()
            )
        },
        token
    ).singleOrNull()
}

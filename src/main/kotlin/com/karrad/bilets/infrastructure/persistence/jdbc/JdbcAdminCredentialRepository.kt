package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.AdminCredential
import com.karrad.bilets.domain.repository.AdminCredentialRepository
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.util.UUID

class JdbcAdminCredentialRepository(private val jdbcTemplate: JdbcTemplate) : AdminCredentialRepository {

    override fun findByUserId(userId: UUID): AdminCredential? = jdbcTemplate.query(
        "select id, user_id, password_hash, created_at, updated_at from admin_credentials where user_id = ?",
        { rs, _ ->
            AdminCredential(
                id = UUID.fromString(rs.getString("id")),
                userId = UUID.fromString(rs.getString("user_id")),
                passwordHash = rs.getString("password_hash"),
                createdAt = rs.getTimestamp("created_at").toInstant(),
                updatedAt = rs.getTimestamp("updated_at").toInstant()
            )
        },
        userId
    ).singleOrNull()

    override fun save(credential: AdminCredential): AdminCredential {
        jdbcTemplate.update(
            """
            insert into admin_credentials (id, user_id, password_hash, created_at, updated_at)
            values (?, ?, ?, ?, ?)
            on conflict (user_id) do update set password_hash = excluded.password_hash, updated_at = excluded.updated_at
            """.trimIndent(),
            credential.id, credential.userId, credential.passwordHash,
            Timestamp.from(credential.createdAt), Timestamp.from(credential.updatedAt)
        )
        return credential
    }
}

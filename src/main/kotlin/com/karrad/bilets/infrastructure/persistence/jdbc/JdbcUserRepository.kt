package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.UserRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

class JdbcUserRepository(
    private val jdbcTemplate: JdbcTemplate
) : UserRepository {

    private val mapper = jacksonObjectMapper()

    override fun save(user: User): User {
        val interestsJson = mapper.writeValueAsString(user.interests)
        val updated = jdbcTemplate.update(
            "update users set email = ?, phone = ?, full_name = ?, role = ?, avatar_url = ?, interests = ? where id = ?",
            user.email, user.phone, user.fullName, user.role.name, user.avatarUrl, interestsJson, user.id
        )
        if (updated == 0) {
            jdbcTemplate.update(
                "insert into users (id, email, phone, full_name, role, avatar_url, interests) values (?, ?, ?, ?, ?, ?, ?)",
                user.id, user.email, user.phone, user.fullName, user.role.name, user.avatarUrl, interestsJson
            )
        }
        return user
    }

    override fun findById(id: UUID): User? = jdbcTemplate.query(
        "select id, email, phone, full_name, role, avatar_url, interests from users where id = ?",
        { rs, _ -> rs.toUser() },
        id
    ).singleOrNull()

    override fun findByEmail(email: String): User? = jdbcTemplate.query(
        "select id, email, phone, full_name, role, avatar_url, interests from users where email = ?",
        { rs, _ -> rs.toUser() },
        email
    ).singleOrNull()

    override fun findByPhone(phone: String): User? = jdbcTemplate.query(
        "select id, email, phone, full_name, role, avatar_url, interests from users where phone = ?",
        { rs, _ -> rs.toUser() },
        phone
    ).singleOrNull()

    override fun findAll(): List<User> = jdbcTemplate.query(
        "select id, email, phone, full_name, role, avatar_url, interests from users order by full_name, id"
    ) { rs, _ -> rs.toUser() }

    override fun deleteById(id: UUID): Boolean =
        jdbcTemplate.update("delete from users where id = ?", id) > 0

    private fun java.sql.ResultSet.toUser() = User(
        id = uuid("id"),
        fullName = getString("full_name"),
        email = getString("email"),
        phone = getString("phone"),
        role = UserRole.valueOf(getString("role")),
        avatarUrl = getString("avatar_url"),
        interests = mapper.readValue(getString("interests") ?: "[]")
    )
}

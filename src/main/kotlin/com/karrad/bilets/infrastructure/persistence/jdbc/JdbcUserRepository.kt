package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.UserRepository
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

class JdbcUserRepository(
    private val jdbcTemplate: JdbcTemplate
) : UserRepository {

    override fun save(user: User): User {
        val updated = jdbcTemplate.update(
            """
            update users
            set email = ?, full_name = ?, role = ?
            where id = ?
            """.trimIndent(),
            user.email,
            user.fullName,
            user.role.name,
            user.id
        )
        if (updated == 0) {
            jdbcTemplate.update(
                """
                insert into users (id, email, full_name, role)
                values (?, ?, ?, ?)
                """.trimIndent(),
                user.id,
                user.email,
                user.fullName,
                user.role.name
            )
        }
        return user
    }

    override fun findById(id: UUID): User? = jdbcTemplate.query(
        """
        select id, email, full_name, role
        from users
        where id = ?
        """.trimIndent(),
        { rs, _ ->
            User(
                email = rs.getString("email"),
                fullName = rs.getString("full_name"),
                role = UserRole.valueOf(rs.getString("role")),
                id = rs.uuid("id")
            )
        },
        id
    ).singleOrNull()

    override fun findByEmail(email: String): User? = jdbcTemplate.query(
        """
        select id, email, full_name, role
        from users
        where email = ?
        """.trimIndent(),
        { rs, _ ->
            User(
                email = rs.getString("email"),
                fullName = rs.getString("full_name"),
                role = UserRole.valueOf(rs.getString("role")),
                id = rs.uuid("id")
            )
        },
        email
    ).singleOrNull()

    override fun findAll(): List<User> = jdbcTemplate.query(
        """
        select id, email, full_name, role
        from users
        order by email, id
        """.trimIndent()
    ) { rs, _ ->
        User(
            email = rs.getString("email"),
            fullName = rs.getString("full_name"),
            role = UserRole.valueOf(rs.getString("role")),
            id = rs.uuid("id")
        )
    }

    override fun deleteById(id: UUID): Boolean = jdbcTemplate.update(
        "delete from users where id = ?",
        id
    ) > 0
}

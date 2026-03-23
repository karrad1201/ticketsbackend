package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.repository.OrganizationRepository
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

class JdbcOrganizationRepository(
    private val jdbcTemplate: JdbcTemplate
) : OrganizationRepository {

    override fun save(organization: Organization): Organization {
        val updated = jdbcTemplate.update(
            """
            update organizations
            set code = ?, name = ?, balance = ?
            where id = ?
            """.trimIndent(),
            organization.code,
            organization.name,
            organization.balance,
            organization.id
        )
        if (updated == 0) {
            jdbcTemplate.update(
                """
                insert into organizations (id, code, name, balance)
                values (?, ?, ?, ?)
                """.trimIndent(),
                organization.id,
                organization.code,
                organization.name,
                organization.balance
            )
        }
        return organization
    }

    override fun findById(id: UUID): Organization? = jdbcTemplate.query(
        """
        select id, code, name, balance
        from organizations
        where id = ?
        """.trimIndent(),
        { rs, _ ->
            Organization(
                code = rs.getString("code"),
                name = rs.getString("name"),
                balance = rs.getInt("balance"),
                id = rs.uuid("id")
            )
        },
        id
    ).singleOrNull()

    override fun findByCode(code: String): Organization? = jdbcTemplate.query(
        """
        select id, code, name, balance
        from organizations
        where code = ?
        """.trimIndent(),
        { rs, _ ->
            Organization(
                code = rs.getString("code"),
                name = rs.getString("name"),
                balance = rs.getInt("balance"),
                id = rs.uuid("id")
            )
        },
        code
    ).singleOrNull()

    override fun findAll(): List<Organization> = jdbcTemplate.query(
        """
        select id, code, name, balance
        from organizations
        order by code, id
        """.trimIndent()
    ) { rs, _ ->
        Organization(
            code = rs.getString("code"),
            name = rs.getString("name"),
            balance = rs.getInt("balance"),
            id = rs.uuid("id")
        )
    }

    override fun deleteById(id: UUID): Boolean = jdbcTemplate.update(
        "delete from organizations where id = ?",
        id
    ) > 0
}

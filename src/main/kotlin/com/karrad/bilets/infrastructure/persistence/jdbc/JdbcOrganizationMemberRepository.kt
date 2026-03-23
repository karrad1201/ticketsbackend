package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

class JdbcOrganizationMemberRepository(
    private val jdbcTemplate: JdbcTemplate
) : OrganizationMemberRepository {

    override fun save(member: OrganizationMember): OrganizationMember {
        val updated = jdbcTemplate.update(
            """
            update organization_members
            set organization_id = ?, user_id = ?, role = ?
            where id = ?
            """.trimIndent(),
            member.organizationId,
            member.userId,
            member.role.name,
            member.id
        )
        if (updated == 0) {
            jdbcTemplate.update(
                """
                insert into organization_members (id, organization_id, user_id, role)
                values (?, ?, ?, ?)
                """.trimIndent(),
                member.id,
                member.organizationId,
                member.userId,
                member.role.name
            )
        }
        return member
    }

    override fun findById(id: UUID): OrganizationMember? = jdbcTemplate.query(
        """
        select id, organization_id, user_id, role
        from organization_members
        where id = ?
        """.trimIndent(),
        { rs, _ -> mapMember(rs) },
        id
    ).singleOrNull()

    override fun findAll(): List<OrganizationMember> = jdbcTemplate.query(
        """
        select id, organization_id, user_id, role
        from organization_members
        order by organization_id, user_id, id
        """.trimIndent()
    ) { rs, _ -> mapMember(rs) }

    override fun findByOrganizationId(organizationId: UUID): List<OrganizationMember> = jdbcTemplate.query(
        """
        select id, organization_id, user_id, role
        from organization_members
        where organization_id = ?
        order by user_id, id
        """.trimIndent(),
        { rs, _ -> mapMember(rs) },
        organizationId
    )

    override fun findByUserId(userId: UUID): List<OrganizationMember> = jdbcTemplate.query(
        """
        select id, organization_id, user_id, role
        from organization_members
        where user_id = ?
        order by organization_id, id
        """.trimIndent(),
        { rs, _ -> mapMember(rs) },
        userId
    )

    override fun findByOrganizationIdAndUserId(organizationId: UUID, userId: UUID): OrganizationMember? = jdbcTemplate.query(
        """
        select id, organization_id, user_id, role
        from organization_members
        where organization_id = ? and user_id = ?
        """.trimIndent(),
        { rs, _ -> mapMember(rs) },
        organizationId,
        userId
    ).singleOrNull()

    override fun deleteById(id: UUID): Boolean = jdbcTemplate.update(
        "delete from organization_members where id = ?",
        id
    ) > 0

    private fun mapMember(rs: java.sql.ResultSet): OrganizationMember = OrganizationMember(
        organizationId = rs.uuid("organization_id"),
        userId = rs.uuid("user_id"),
        role = OrganizationMemberRole.valueOf(rs.getString("role")),
        id = rs.uuid("id")
    )
}

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
        jdbcTemplate.update(
            """
            insert into organization_members (id, organization_id, user_id, role, venue_id)
            values (?, ?, ?, ?, ?)
            on conflict (id) do update set
              organization_id = excluded.organization_id,
              user_id         = excluded.user_id,
              role            = excluded.role,
              venue_id        = excluded.venue_id
            """.trimIndent(),
            member.id, member.organizationId, member.userId, member.role.name, member.venueId
        )
        return member
    }

    override fun findById(id: UUID): OrganizationMember? = jdbcTemplate.query(
        "select id, organization_id, user_id, role, venue_id from organization_members where id = ?",
        { rs, _ -> mapMember(rs) },
        id
    ).singleOrNull()

    override fun findAll(): List<OrganizationMember> = jdbcTemplate.query(
        "select id, organization_id, user_id, role, venue_id from organization_members order by organization_id, user_id, id"
    ) { rs, _ -> mapMember(rs) }

    override fun findByOrganizationId(organizationId: UUID): List<OrganizationMember> = jdbcTemplate.query(
        "select id, organization_id, user_id, role, venue_id from organization_members where organization_id = ? order by user_id, id",
        { rs, _ -> mapMember(rs) },
        organizationId
    )

    override fun findByUserId(userId: UUID): List<OrganizationMember> = jdbcTemplate.query(
        "select id, organization_id, user_id, role, venue_id from organization_members where user_id = ? order by organization_id, id",
        { rs, _ -> mapMember(rs) },
        userId
    )

    override fun findByOrganizationIdAndUserId(organizationId: UUID, userId: UUID): OrganizationMember? = jdbcTemplate.query(
        "select id, organization_id, user_id, role, venue_id from organization_members where organization_id = ? and user_id = ?",
        { rs, _ -> mapMember(rs) },
        organizationId,
        userId
    ).singleOrNull()

    override fun findByOrganizationIdAndRole(organizationId: UUID, role: OrganizationMemberRole): List<OrganizationMember> = jdbcTemplate.query(
        "select id, organization_id, user_id, role, venue_id from organization_members where organization_id = ? and role = ? order by user_id, id",
        { rs, _ -> mapMember(rs) },
        organizationId,
        role.name
    )

    override fun deleteById(id: UUID): Boolean = jdbcTemplate.update(
        "delete from organization_members where id = ?",
        id
    ) > 0

    private fun mapMember(rs: java.sql.ResultSet): OrganizationMember = OrganizationMember(
        organizationId = rs.uuid("organization_id"),
        userId = rs.uuid("user_id"),
        role = OrganizationMemberRole.valueOf(rs.getString("role")),
        venueId = rs.nullableUuid("venue_id"),
        id = rs.uuid("id")
    )
}

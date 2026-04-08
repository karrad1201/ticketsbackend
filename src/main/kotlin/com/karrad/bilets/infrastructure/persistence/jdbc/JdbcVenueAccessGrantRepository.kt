package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.VenueAccessGrant
import com.karrad.bilets.domain.enums.VenueAccessGrantStatus
import com.karrad.bilets.domain.repository.VenueAccessGrantRepository
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.util.UUID

class JdbcVenueAccessGrantRepository(
    private val jdbcTemplate: JdbcTemplate
) : VenueAccessGrantRepository {

    private fun rowMapper(rs: java.sql.ResultSet) = VenueAccessGrant(
        id = rs.uuid("id"),
        venueId = rs.uuid("venue_id"),
        requestingOrgId = rs.uuid("requesting_org_id"),
        status = VenueAccessGrantStatus.valueOf(rs.getString("status")),
        createdAt = rs.instant("created_at"),
        decidedAt = rs.nullableInstant("decided_at"),
        decidedBy = rs.nullableUuid("decided_by")
    )

    private val selectAll = """
        select id, venue_id, requesting_org_id, status, created_at, decided_at, decided_by
        from venue_access_grants
    """.trimIndent()

    override fun save(grant: VenueAccessGrant): VenueAccessGrant {
        val updated = jdbcTemplate.update(
            """
            update venue_access_grants
            set status = ?, decided_at = ?, decided_by = ?
            where id = ?
            """.trimIndent(),
            grant.status.name,
            instantToTimestamp(grant.decidedAt),
            grant.decidedBy,
            grant.id
        )
        if (updated == 0) {
            jdbcTemplate.update(
                """
                insert into venue_access_grants (id, venue_id, requesting_org_id, status, created_at, decided_at, decided_by)
                values (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                grant.id,
                grant.venueId,
                grant.requestingOrgId,
                grant.status.name,
                Timestamp.from(grant.createdAt),
                instantToTimestamp(grant.decidedAt),
                grant.decidedBy
            )
        }
        return grant
    }

    override fun findById(id: UUID): VenueAccessGrant? = jdbcTemplate.query(
        "$selectAll\nwhere id = ?",
        { rs, _ -> rowMapper(rs) },
        id
    ).singleOrNull()

    override fun findByVenueId(venueId: UUID): List<VenueAccessGrant> = jdbcTemplate.query(
        "$selectAll\nwhere venue_id = ?\norder by created_at desc",
        { rs, _ -> rowMapper(rs) },
        venueId
    )

    override fun findByVenueIdAndStatus(venueId: UUID, status: VenueAccessGrantStatus): List<VenueAccessGrant> =
        jdbcTemplate.query(
            "$selectAll\nwhere venue_id = ? and status = ?\norder by created_at desc",
            { rs, _ -> rowMapper(rs) },
            venueId,
            status.name
        )

    override fun findApprovedByVenueIdAndOrgId(venueId: UUID, orgId: UUID): VenueAccessGrant? =
        jdbcTemplate.query(
            "$selectAll\nwhere venue_id = ? and requesting_org_id = ? and status = 'APPROVED'",
            { rs, _ -> rowMapper(rs) },
            venueId,
            orgId
        ).singleOrNull()
}

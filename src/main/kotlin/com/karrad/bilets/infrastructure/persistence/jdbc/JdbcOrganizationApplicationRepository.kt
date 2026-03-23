package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.OrganizationApplication
import com.karrad.bilets.domain.enums.OrganizationApplicationStatus
import com.karrad.bilets.domain.repository.OrganizationApplicationRepository
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

class JdbcOrganizationApplicationRepository(
    private val jdbcTemplate: JdbcTemplate
) : OrganizationApplicationRepository {

    override fun save(application: OrganizationApplication): OrganizationApplication {
        val updated = jdbcTemplate.update(
            """
            update organization_applications
            set applicant_user_id = ?, organization_code = ?, organization_name = ?, status = ?,
                reviewed_by_user_id = ?, reviewed_at = ?, organization_id = ?
            where id = ?
            """.trimIndent(),
            application.applicantUserId,
            application.organizationCode,
            application.organizationName,
            application.status.name,
            application.reviewedByUserId,
            instantToTimestamp(application.reviewedAt),
            application.organizationId,
            application.id
        )
        if (updated == 0) {
            jdbcTemplate.update(
                """
                insert into organization_applications (
                    id, applicant_user_id, organization_code, organization_name, status,
                    reviewed_by_user_id, reviewed_at, organization_id
                ) values (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                application.id,
                application.applicantUserId,
                application.organizationCode,
                application.organizationName,
                application.status.name,
                application.reviewedByUserId,
                instantToTimestamp(application.reviewedAt),
                application.organizationId
            )
        }
        return application
    }

    override fun findById(id: UUID): OrganizationApplication? = jdbcTemplate.query(
        """
        select id, applicant_user_id, organization_code, organization_name, status,
               reviewed_by_user_id, reviewed_at, organization_id
        from organization_applications
        where id = ?
        """.trimIndent(),
        { rs, _ -> mapApplication(rs) },
        id
    ).singleOrNull()

    override fun findAll(): List<OrganizationApplication> = jdbcTemplate.query(
        """
        select id, applicant_user_id, organization_code, organization_name, status,
               reviewed_by_user_id, reviewed_at, organization_id
        from organization_applications
        order by organization_code, id
        """.trimIndent()
    ) { rs, _ -> mapApplication(rs) }

    override fun findPendingByOrganizationCode(code: String): OrganizationApplication? = jdbcTemplate.query(
        """
        select id, applicant_user_id, organization_code, organization_name, status,
               reviewed_by_user_id, reviewed_at, organization_id
        from organization_applications
        where organization_code = ? and status = ?
        """.trimIndent(),
        { rs, _ -> mapApplication(rs) },
        code,
        OrganizationApplicationStatus.PENDING.name
    ).singleOrNull()

    override fun deleteById(id: UUID): Boolean = jdbcTemplate.update(
        "delete from organization_applications where id = ?",
        id
    ) > 0

    private fun mapApplication(rs: java.sql.ResultSet): OrganizationApplication =
        OrganizationApplication(
            applicantUserId = rs.uuid("applicant_user_id"),
            organizationCode = rs.getString("organization_code"),
            organizationName = rs.getString("organization_name"),
            status = OrganizationApplicationStatus.valueOf(rs.getString("status")),
            reviewedByUserId = rs.nullableUuid("reviewed_by_user_id"),
            reviewedAt = rs.nullableInstant("reviewed_at"),
            organizationId = rs.nullableUuid("organization_id"),
            id = rs.uuid("id")
        )
}

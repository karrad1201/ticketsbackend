package com.karrad.bilets.infrastructure.persistence.jdbc

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.karrad.bilets.domain.entity.OrganizationApplication
import com.karrad.bilets.domain.enums.OrganizationApplicationStatus
import com.karrad.bilets.domain.repository.OrganizationApplicationRepository
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

class JdbcOrganizationApplicationRepository(
    private val jdbcTemplate: JdbcTemplate
) : OrganizationApplicationRepository {

    private val mapper = jacksonObjectMapper()

    override fun save(application: OrganizationApplication): OrganizationApplication {
        val documentUrlsJson = mapper.writeValueAsString(application.documentUrls)
        jdbcTemplate.update(
            """
            insert into organization_applications (
                id, applicant_user_id, organization_code, organization_name,
                contact_email, contact_phone, document_urls, status,
                reviewed_by_user_id, reviewed_at, organization_id
            ) values (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?)
            on conflict (id) do update set
              applicant_user_id = excluded.applicant_user_id,
              organization_code = excluded.organization_code,
              organization_name = excluded.organization_name,
              contact_email     = excluded.contact_email,
              contact_phone     = excluded.contact_phone,
              document_urls     = excluded.document_urls,
              status = excluded.status,
              reviewed_by_user_id = excluded.reviewed_by_user_id,
              reviewed_at = excluded.reviewed_at,
              organization_id = excluded.organization_id
            """.trimIndent(),
            application.id, application.applicantUserId, application.organizationCode,
            application.organizationName, application.contactEmail, application.contactPhone,
            documentUrlsJson, application.status.name,
            application.reviewedByUserId, instantToTimestamp(application.reviewedAt), application.organizationId
        )
        return application
    }

    override fun findById(id: UUID): OrganizationApplication? = jdbcTemplate.query(
        """
        select id, applicant_user_id, organization_code, organization_name,
               contact_email, contact_phone, document_urls, status,
               reviewed_by_user_id, reviewed_at, organization_id
        from organization_applications
        where id = ?
        """.trimIndent(),
        { rs, _ -> mapApplication(rs) },
        id
    ).singleOrNull()

    override fun findAll(): List<OrganizationApplication> = jdbcTemplate.query(
        """
        select id, applicant_user_id, organization_code, organization_name,
               contact_email, contact_phone, document_urls, status,
               reviewed_by_user_id, reviewed_at, organization_id
        from organization_applications
        order by organization_code, id
        """.trimIndent()
    ) { rs, _ -> mapApplication(rs) }

    override fun findPendingByOrganizationCode(code: String): OrganizationApplication? = jdbcTemplate.query(
        """
        select id, applicant_user_id, organization_code, organization_name,
               contact_email, contact_phone, document_urls, status,
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

    private fun mapApplication(rs: java.sql.ResultSet): OrganizationApplication {
        val docUrls: List<String> = rs.getString("document_urls")
            ?.let { mapper.readValue(it) } ?: emptyList()
        return OrganizationApplication(
            applicantUserId = rs.uuid("applicant_user_id"),
            organizationCode = rs.getString("organization_code"),
            organizationName = rs.getString("organization_name"),
            contactEmail = rs.getString("contact_email"),
            contactPhone = rs.getString("contact_phone"),
            documentUrls = docUrls,
            status = OrganizationApplicationStatus.valueOf(rs.getString("status")),
            reviewedByUserId = rs.nullableUuid("reviewed_by_user_id"),
            reviewedAt = rs.nullableInstant("reviewed_at"),
            organizationId = rs.nullableUuid("organization_id"),
            id = rs.uuid("id")
        )
    }
}

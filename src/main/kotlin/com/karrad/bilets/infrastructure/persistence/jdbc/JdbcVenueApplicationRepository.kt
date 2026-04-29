package com.karrad.bilets.infrastructure.persistence.jdbc

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.karrad.bilets.domain.entity.VenueApplication
import com.karrad.bilets.domain.enums.VenueApplicationStatus
import com.karrad.bilets.domain.repository.VenueApplicationRepository
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.ResultSet
import java.util.UUID

class JdbcVenueApplicationRepository(
    private val jdbcTemplate: JdbcTemplate
) : VenueApplicationRepository {

    private val mapper = jacksonObjectMapper()

    override fun save(application: VenueApplication): VenueApplication {
        val documentUrlsJson = mapper.writeValueAsString(application.documentUrls)
        jdbcTemplate.update(
            """
            insert into venue_applications (
                id, organization_id, applicant_user_id, name, city_label, subject_label,
                address, description, document_urls, status, reviewed_by_user_id,
                reviewed_at, venue_id, created_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            on conflict (id) do update set
                organization_id     = excluded.organization_id,
                applicant_user_id   = excluded.applicant_user_id,
                name                = excluded.name,
                city_label          = excluded.city_label,
                subject_label       = excluded.subject_label,
                address             = excluded.address,
                description         = excluded.description,
                document_urls       = excluded.document_urls,
                status              = excluded.status,
                reviewed_by_user_id = excluded.reviewed_by_user_id,
                reviewed_at         = excluded.reviewed_at,
                venue_id            = excluded.venue_id,
                created_at          = excluded.created_at
            """.trimIndent(),
            application.id,
            application.organizationId,
            application.applicantUserId,
            application.name,
            application.cityLabel,
            application.subjectLabel,
            application.address,
            application.description,
            documentUrlsJson,
            application.status.name,
            application.reviewedByUserId,
            instantToTimestamp(application.reviewedAt),
            application.venueId,
            instantToTimestamp(application.createdAt)
        )
        return application
    }

    override fun findById(id: UUID): VenueApplication? = jdbcTemplate.query(
        "select * from venue_applications where id = ?",
        { rs, _ -> mapRow(rs) },
        id
    ).singleOrNull()

    override fun findByOrganizationId(organizationId: UUID): List<VenueApplication> = jdbcTemplate.query(
        "select * from venue_applications where organization_id = ? order by created_at, id",
        { rs, _ -> mapRow(rs) },
        organizationId
    )

    override fun findByStatus(status: VenueApplicationStatus): List<VenueApplication> = jdbcTemplate.query(
        "select * from venue_applications where status = ? order by created_at, id",
        { rs, _ -> mapRow(rs) },
        status.name
    )

    override fun findAll(): List<VenueApplication> = jdbcTemplate.query(
        "select * from venue_applications order by created_at, id"
    ) { rs, _ -> mapRow(rs) }

    override fun deleteById(id: UUID): Boolean = jdbcTemplate.update(
        "delete from venue_applications where id = ?",
        id
    ) > 0

    private fun mapRow(rs: ResultSet): VenueApplication = VenueApplication(
        id = rs.uuid("id"),
        organizationId = rs.uuid("organization_id"),
        applicantUserId = rs.uuid("applicant_user_id"),
        name = rs.getString("name"),
        cityLabel = rs.getString("city_label"),
        subjectLabel = rs.getString("subject_label"),
        address = rs.getString("address"),
        description = rs.getString("description"),
        documentUrls = mapper.readValue(rs.getString("document_urls") ?: "[]"),
        status = VenueApplicationStatus.valueOf(rs.getString("status")),
        reviewedByUserId = rs.nullableUuid("reviewed_by_user_id"),
        reviewedAt = rs.nullableInstant("reviewed_at"),
        venueId = rs.nullableUuid("venue_id"),
        createdAt = rs.instant("created_at")
    )
}

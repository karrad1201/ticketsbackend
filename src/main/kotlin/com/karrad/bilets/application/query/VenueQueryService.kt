package com.karrad.bilets.application.query

import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.VenueSpace
import com.karrad.bilets.infrastructure.persistence.jdbc.nullableUuid
import com.karrad.bilets.infrastructure.persistence.jdbc.uuid
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.sql.ResultSet
import java.util.LinkedHashMap
import java.util.UUID

@Service
@ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "jdbc")
class VenueQueryService(private val jdbcTemplate: JdbcTemplate) : VenueQueryPort {

    override fun findAll(): List<Venue> = jdbcTemplate.query(
        """
        SELECT v.id AS v_id, v.label AS v_label, v.city_label, v.subject_label,
               v.organization_id, v.address,
               c.id AS city_id, sub.id AS subject_id,
               s.id AS space_id, s.label AS space_label
        FROM venues v
        LEFT JOIN cities c ON c.label = v.city_label
        LEFT JOIN subjects sub ON sub.label = v.subject_label
        LEFT JOIN venue_spaces s ON s.venue_id = v.id
        ORDER BY v.label, v.id, s.label, s.id
        """.trimIndent(),
        venueExtractor()
    ) ?: emptyList()

    override fun findById(id: UUID): Venue? = jdbcTemplate.query(
        """
        SELECT v.id AS v_id, v.label AS v_label, v.city_label, v.subject_label,
               v.organization_id, v.address,
               c.id AS city_id, sub.id AS subject_id,
               s.id AS space_id, s.label AS space_label
        FROM venues v
        LEFT JOIN cities c ON c.label = v.city_label
        LEFT JOIN subjects sub ON sub.label = v.subject_label
        LEFT JOIN venue_spaces s ON s.venue_id = v.id
        WHERE v.id = ?
        ORDER BY s.label, s.id
        """.trimIndent(),
        venueExtractor(),
        id
    )?.singleOrNull()

    private fun venueExtractor(): ResultSetExtractor<List<Venue>> = ResultSetExtractor { rs ->
        val map = LinkedHashMap<UUID, Pair<VenueRow, MutableList<VenueSpace>>>()
        while (rs.next()) {
            val venueId = rs.uuid("v_id")
            val entry = map.getOrPut(venueId) { Pair(extractVenueRow(rs), mutableListOf()) }
            val spaceId = rs.nullableUuid("space_id")
            if (spaceId != null) {
                entry.second.add(VenueSpace(id = spaceId, label = rs.getString("space_label")))
            }
        }
        map.values.map { (row, spaces) ->
            Venue(
                id = row.id,
                label = row.label,
                city = City(
                    id = row.cityId ?: UUID.randomUUID(),
                    label = row.cityLabel,
                    subject = Subject(id = row.subjectId ?: UUID.randomUUID(), label = row.subjectLabel)
                ),
                organizationId = row.organizationId,
                address = row.address,
                spaces = spaces
            )
        }
    }

    private fun extractVenueRow(rs: ResultSet) = VenueRow(
        id = rs.uuid("v_id"),
        label = rs.getString("v_label"),
        cityLabel = rs.getString("city_label"),
        cityId = rs.nullableUuid("city_id"),
        subjectLabel = rs.getString("subject_label"),
        subjectId = rs.nullableUuid("subject_id"),
        organizationId = rs.nullableUuid("organization_id"),
        address = rs.getString("address")
    )

    private data class VenueRow(
        val id: UUID,
        val label: String,
        val cityLabel: String,
        val cityId: UUID?,
        val subjectLabel: String,
        val subjectId: UUID?,
        val organizationId: UUID?,
        val address: String?
    )
}

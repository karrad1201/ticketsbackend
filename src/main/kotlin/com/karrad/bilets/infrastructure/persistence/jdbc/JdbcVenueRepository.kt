package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.VenueSpace
import com.karrad.bilets.domain.enums.VenueSpaceType
import com.karrad.bilets.domain.repository.VenueRepository
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

class JdbcVenueRepository(
    private val jdbcTemplate: JdbcTemplate
) : VenueRepository {

    override fun save(venue: Venue): Venue {
        jdbcTemplate.update(
            """
            insert into venues (id, label, city_label, subject_label, organization_id, address)
            values (?, ?, ?, ?, ?, ?)
            on conflict (id) do update set
              label = excluded.label, city_label = excluded.city_label,
              subject_label = excluded.subject_label, organization_id = excluded.organization_id,
              address = excluded.address
            """.trimIndent(),
            venue.id, venue.label, venue.city.label, venue.city.subject.label,
            venue.organizationId, venue.address
        )

        jdbcTemplate.update("delete from venue_spaces where venue_id = ?", venue.id)
        venue.spaces.forEach { space ->
            jdbcTemplate.update(
                """
                insert into venue_spaces (id, venue_id, label, type, capacity)
                values (?, ?, ?, ?, ?)
                """.trimIndent(),
                space.id,
                venue.id,
                space.label,
                space.type.name,
                space.capacity
            )
        }

        return venue
    }

    override fun findById(id: UUID): Venue? = jdbcTemplate.query(
        """
        select id, label, city_label, subject_label, organization_id, address
        from venues
        where id = ?
        """.trimIndent(),
        { rs, _ -> mapVenue(rs) },
        id
    ).singleOrNull()

    override fun findAllByIds(ids: Collection<UUID>): List<Venue> {
        if (ids.isEmpty()) return emptyList()
        return jdbcTemplate.query(
            { conn ->
                conn.prepareStatement(
                    "select id, label, city_label, subject_label, organization_id, address from venues where id = ANY(?)"
                ).apply {
                    setArray(1, conn.createArrayOf("uuid", ids.toTypedArray()))
                }
            },
            { rs, _ -> mapVenue(rs) }
        )
    }

    override fun findBySpaceId(spaceId: UUID): Venue? = jdbcTemplate.query(
        """
        select v.id, v.label, v.city_label, v.subject_label, v.organization_id, v.address
        from venues v
        join venue_spaces s on s.venue_id = v.id
        where s.id = ?
        """.trimIndent(),
        { rs, _ -> mapVenue(rs) },
        spaceId
    ).singleOrNull()

    override fun findAll(): List<Venue> {
        data class VenueRow(
            val id: UUID, val label: String, val cityLabel: String,
            val subjectLabel: String, val organizationId: UUID?, val address: String?
        )

        val rows = jdbcTemplate.query(
            "select id, label, city_label, subject_label, organization_id, address from venues order by label, id"
        ) { rs, _ ->
            VenueRow(
                id = rs.uuid("id"), label = rs.getString("label"),
                cityLabel = rs.getString("city_label"), subjectLabel = rs.getString("subject_label"),
                organizationId = rs.nullableUuid("organization_id"), address = rs.getString("address")
            )
        }
        if (rows.isEmpty()) return emptyList()

        val placeholders = rows.joinToString(",") { "?" }
        val spacesById: Map<UUID, List<VenueSpace>> = jdbcTemplate.query(
            "select venue_id, id, label, type, capacity from venue_spaces where venue_id in ($placeholders) order by label, id",
            { rs, _ -> Pair(rs.uuid("venue_id"), mapSpace(rs)) },
            *rows.map { it.id }.toTypedArray()
        ).groupBy({ it.first }, { it.second })

        return rows.map { r ->
            Venue(
                label = r.label,
                city = City(label = r.cityLabel, subject = Subject(r.subjectLabel)),
                organizationId = r.organizationId,
                id = r.id,
                spaces = spacesById[r.id] ?: emptyList(),
                address = r.address
            )
        }
    }

    override fun findByOrganizationId(organizationId: UUID): List<Venue> = jdbcTemplate.query(
        "select id, label, city_label, subject_label, organization_id, address from venues where organization_id = ? order by label, id",
        { rs, _ -> mapVenue(rs) },
        organizationId
    )

    override fun deleteById(id: UUID): Boolean {
        jdbcTemplate.update("delete from venue_spaces where venue_id = ?", id)
        return jdbcTemplate.update("delete from venues where id = ?", id) > 0
    }

    private fun mapVenue(rs: java.sql.ResultSet): Venue {
        val venueId = rs.uuid("id")
        return Venue(
            label = rs.getString("label"),
            city = City(
                label = rs.getString("city_label"),
                subject = Subject(rs.getString("subject_label"))
            ),
            organizationId = rs.nullableUuid("organization_id"),
            id = venueId,
            spaces = findSpaces(venueId),
            address = rs.getString("address")
        )
    }

    override fun addSpace(venueId: UUID, space: VenueSpace): VenueSpace {
        jdbcTemplate.update(
            """
            insert into venue_spaces (id, venue_id, label, type, capacity)
            values (?, ?, ?, ?, ?)
            on conflict (id) do update set
              label = excluded.label, type = excluded.type, capacity = excluded.capacity
            """.trimIndent(),
            space.id, venueId, space.label, space.type.name, space.capacity
        )
        return space
    }

    private fun findSpaces(venueId: UUID): List<VenueSpace> = jdbcTemplate.query(
        """
        select id, label, type, capacity
        from venue_spaces
        where venue_id = ?
        order by label, id
        """.trimIndent(),
        { rs, _ -> mapSpace(rs) },
        venueId
    )

    private fun mapSpace(rs: java.sql.ResultSet) = VenueSpace(
        label = rs.getString("label"),
        type = VenueSpaceType.valueOf(rs.getString("type")),
        capacity = rs.getInt("capacity"),
        id = rs.uuid("id")
    )
}

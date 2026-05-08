package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.InventoryMode
import com.karrad.bilets.domain.entity.SectionPrice
import com.karrad.bilets.domain.entity.SpacePriceProfile
import com.karrad.bilets.domain.entity.TicketTypeTemplate
import com.karrad.bilets.domain.repository.SpacePriceProfileRepository
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

class JdbcSpacePriceProfileRepository(
    private val jdbcTemplate: JdbcTemplate
) : SpacePriceProfileRepository {

    override fun save(profile: SpacePriceProfile): SpacePriceProfile {
        jdbcTemplate.update(
            """
            insert into space_price_profiles (id, venue_space_id, label, mode)
            values (?, ?, ?, ?)
            on conflict (id) do update set
              venue_space_id = excluded.venue_space_id,
              label = excluded.label,
              mode = excluded.mode
            """.trimIndent(),
            profile.id, profile.venueSpaceId, profile.label, profile.mode.name
        )

        jdbcTemplate.update("delete from space_price_profile_sections where profile_id = ?", profile.id)
        profile.sectionPrices.forEachIndexed { i, sp ->
            jdbcTemplate.update(
                "insert into space_price_profile_sections (profile_id, section_key, price, sort_order) values (?, ?, ?, ?)",
                profile.id, sp.sectionKey, sp.price, i
            )
        }

        jdbcTemplate.update("delete from space_price_profile_ticket_types where profile_id = ?", profile.id)
        profile.ticketTypes.forEachIndexed { i, tt ->
            jdbcTemplate.update(
                "insert into space_price_profile_ticket_types (profile_id, label, price, quota, sort_order) values (?, ?, ?, ?, ?)",
                profile.id, tt.label, tt.price, tt.quota, i
            )
        }

        return profile
    }

    override fun findById(id: UUID): SpacePriceProfile? = jdbcTemplate.query(
        "select id, venue_space_id, label, mode from space_price_profiles where id = ?",
        { rs, _ ->
            val profileId = rs.uuid("id")
            SpacePriceProfile(
                id = profileId,
                venueSpaceId = rs.uuid("venue_space_id"),
                label = rs.getString("label"),
                mode = InventoryMode.valueOf(rs.getString("mode")),
                sectionPrices = findSectionPrices(profileId),
                ticketTypes = findTicketTypes(profileId)
            )
        },
        id
    ).singleOrNull()

    override fun findByVenueSpaceId(venueSpaceId: UUID): List<SpacePriceProfile> {
        val profiles = jdbcTemplate.query(
            "select id, venue_space_id, label, mode from space_price_profiles where venue_space_id = ? order by label, id",
            { rs, _ -> Triple(rs.uuid("id"), rs.getString("label"), rs.getString("mode")) },
            venueSpaceId
        )
        if (profiles.isEmpty()) return emptyList()

        val ids = profiles.map { it.first }
        val sectionsByProfile = loadSectionPrices(ids)
        val ticketTypesByProfile = loadTicketTypes(ids)

        return profiles.map { (id, label, mode) ->
            SpacePriceProfile(
                id = id,
                venueSpaceId = venueSpaceId,
                label = label,
                mode = InventoryMode.valueOf(mode),
                sectionPrices = sectionsByProfile[id] ?: emptyList(),
                ticketTypes = ticketTypesByProfile[id] ?: emptyList()
            )
        }
    }

    override fun deleteById(id: UUID): Boolean {
        jdbcTemplate.update("delete from space_price_profile_sections where profile_id = ?", id)
        jdbcTemplate.update("delete from space_price_profile_ticket_types where profile_id = ?", id)
        return jdbcTemplate.update("delete from space_price_profiles where id = ?", id) > 0
    }

    private fun findSectionPrices(profileId: UUID): List<SectionPrice> = jdbcTemplate.query(
        "select section_key, price from space_price_profile_sections where profile_id = ? order by sort_order",
        { rs, _ -> SectionPrice(sectionKey = rs.getString("section_key"), price = rs.getInt("price")) },
        profileId
    )

    private fun findTicketTypes(profileId: UUID): List<TicketTypeTemplate> = jdbcTemplate.query(
        "select label, price, quota from space_price_profile_ticket_types where profile_id = ? order by sort_order",
        { rs, _ -> TicketTypeTemplate(label = rs.getString("label"), price = rs.getInt("price"), quota = rs.getInt("quota")) },
        profileId
    )

    private fun loadSectionPrices(profileIds: List<UUID>): Map<UUID, List<SectionPrice>> {
        if (profileIds.isEmpty()) return emptyMap()
        return jdbcTemplate.query(
            { conn ->
                conn.prepareStatement(
                    "select profile_id, section_key, price from space_price_profile_sections where profile_id = ANY(?) order by profile_id, sort_order"
                ).apply { setArray(1, conn.createArrayOf("uuid", profileIds.toTypedArray())) }
            },
            { rs, _ -> rs.uuid("profile_id") to SectionPrice(rs.getString("section_key"), rs.getInt("price")) }
        ).groupBy({ it.first }, { it.second })
    }

    private fun loadTicketTypes(profileIds: List<UUID>): Map<UUID, List<TicketTypeTemplate>> {
        if (profileIds.isEmpty()) return emptyMap()
        return jdbcTemplate.query(
            { conn ->
                conn.prepareStatement(
                    "select profile_id, label, price, quota from space_price_profile_ticket_types where profile_id = ANY(?) order by profile_id, sort_order"
                ).apply { setArray(1, conn.createArrayOf("uuid", profileIds.toTypedArray())) }
            },
            { rs, _ -> rs.uuid("profile_id") to TicketTypeTemplate(rs.getString("label"), rs.getInt("price"), rs.getInt("quota")) }
        ).groupBy({ it.first }, { it.second })
    }
}

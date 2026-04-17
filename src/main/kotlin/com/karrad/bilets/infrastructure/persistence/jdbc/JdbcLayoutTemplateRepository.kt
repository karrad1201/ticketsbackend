package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.LayoutTemplate
import com.karrad.bilets.domain.entity.Row
import com.karrad.bilets.domain.entity.Section
import com.karrad.bilets.domain.repository.LayoutTemplateRepository
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

class JdbcLayoutTemplateRepository(
    private val jdbcTemplate: JdbcTemplate
) : LayoutTemplateRepository {

    override fun save(layoutTemplate: LayoutTemplate): LayoutTemplate {
        jdbcTemplate.update(
            """
            insert into layout_templates (id, venue_space_id, label)
            values (?, ?, ?)
            on conflict (id) do update set venue_space_id = excluded.venue_space_id, label = excluded.label
            """.trimIndent(),
            layoutTemplate.id, layoutTemplate.venueSpaceId, layoutTemplate.label
        )

        jdbcTemplate.update("delete from layout_template_rows where layout_template_id = ?", layoutTemplate.id)
        jdbcTemplate.update("delete from layout_template_sections where layout_template_id = ?", layoutTemplate.id)

        layoutTemplate.sections.forEachIndexed { sectionIndex, section ->
            jdbcTemplate.update(
                """
                insert into layout_template_sections (layout_template_id, section_key, label, sort_order)
                values (?, ?, ?, ?)
                """.trimIndent(),
                layoutTemplate.id,
                section.key,
                section.label,
                sectionIndex
            )

            section.rows.forEachIndexed { rowIndex, row ->
                jdbcTemplate.update(
                    """
                    insert into layout_template_rows (
                        layout_template_id, section_key, row_key, label, start_seat, end_seat, price, sort_order
                    ) values (?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    layoutTemplate.id,
                    section.key,
                    row.key,
                    row.label,
                    row.startSeat,
                    row.endSeat,
                    row.price,
                    rowIndex
                )
            }
        }

        return layoutTemplate
    }

    override fun findById(id: UUID): LayoutTemplate? = jdbcTemplate.query(
        """
        select id, venue_space_id, label
        from layout_templates
        where id = ?
        """.trimIndent(),
        { rs, _ ->
            val templateId = rs.uuid("id")
            LayoutTemplate(
                venueSpaceId = rs.uuid("venue_space_id"),
                label = rs.getString("label"),
                sections = findSections(templateId),
                id = templateId
            )
        },
        id
    ).singleOrNull()

    override fun findAll(): List<LayoutTemplate> {
        val templates = jdbcTemplate.query(
            """
            select id, venue_space_id, label
            from layout_templates
            order by label, id
            """.trimIndent()
        ) { rs, _ -> Triple(rs.uuid("id"), rs.uuid("venue_space_id"), rs.getString("label")) }

        if (templates.isEmpty()) return emptyList()

        val rowsByTemplate = loadAllRows()
        val sectionsByTemplate = loadAllSections()
        return templates.map { (id, venueSpaceId, label) ->
            assembleTemplate(id, venueSpaceId, label, rowsByTemplate, sectionsByTemplate)
        }
    }

    override fun findByVenueSpaceId(venueSpaceId: UUID): List<LayoutTemplate> {
        val templates = jdbcTemplate.query(
            """
            select id, venue_space_id, label
            from layout_templates
            where venue_space_id = ?
            order by label, id
            """.trimIndent(),
            { rs, _ -> Triple(rs.uuid("id"), rs.uuid("venue_space_id"), rs.getString("label")) },
            venueSpaceId
        )

        if (templates.isEmpty()) return emptyList()

        val rowsByTemplate = loadRowsByVenueSpaceId(venueSpaceId)
        val sectionsByTemplate = loadSectionsByVenueSpaceId(venueSpaceId)
        return templates.map { (id, vsId, label) ->
            assembleTemplate(id, vsId, label, rowsByTemplate, sectionsByTemplate)
        }
    }

    override fun deleteById(id: UUID): Boolean {
        jdbcTemplate.update("delete from layout_template_rows where layout_template_id = ?", id)
        jdbcTemplate.update("delete from layout_template_sections where layout_template_id = ?", id)
        return jdbcTemplate.update("delete from layout_templates where id = ?", id) > 0
    }

    private fun assembleTemplate(
        id: UUID,
        venueSpaceId: UUID,
        label: String,
        rowsByTemplate: Map<UUID, Map<String, List<Row>>>,
        sectionsByTemplate: Map<UUID, List<Pair<String, String>>>
    ): LayoutTemplate {
        val sectionRows = rowsByTemplate[id] ?: emptyMap()
        val sections = (sectionsByTemplate[id] ?: emptyList()).map { (sectionKey, sectionLabel) ->
            Section(label = sectionLabel, key = sectionKey, rows = sectionRows[sectionKey].orEmpty())
        }
        return LayoutTemplate(venueSpaceId = venueSpaceId, label = label, sections = sections, id = id)
    }

    private fun loadAllRows(): Map<UUID, Map<String, List<Row>>> =
        jdbcTemplate.query(
            """
            select layout_template_id, section_key, row_key, label, start_seat, end_seat, price
            from layout_template_rows
            order by layout_template_id, section_key, sort_order, row_key
            """.trimIndent()
        ) { rs, _ ->
            Triple(rs.uuid("layout_template_id"), rs.getString("section_key"), mapRow(rs))
        }.groupBy({ it.first }, { it.second to it.third })
            .mapValues { (_, pairs) -> pairs.groupBy({ it.first }, { it.second }) }

    private fun loadAllSections(): Map<UUID, List<Pair<String, String>>> =
        jdbcTemplate.query(
            """
            select layout_template_id, section_key, label
            from layout_template_sections
            order by layout_template_id, sort_order, section_key
            """.trimIndent()
        ) { rs, _ ->
            Triple(rs.uuid("layout_template_id"), rs.getString("section_key"), rs.getString("label"))
        }.groupBy({ it.first }, { it.second to it.third })

    private fun loadRowsByVenueSpaceId(venueSpaceId: UUID): Map<UUID, Map<String, List<Row>>> =
        jdbcTemplate.query(
            """
            select r.layout_template_id, r.section_key, r.row_key, r.label, r.start_seat, r.end_seat, r.price
            from layout_template_rows r
            join layout_templates lt on lt.id = r.layout_template_id
            where lt.venue_space_id = ?
            order by r.layout_template_id, r.section_key, r.sort_order, r.row_key
            """.trimIndent(),
            { rs, _ -> Triple(rs.uuid("layout_template_id"), rs.getString("section_key"), mapRow(rs)) },
            venueSpaceId
        ).groupBy({ it.first }, { it.second to it.third })
            .mapValues { (_, pairs) -> pairs.groupBy({ it.first }, { it.second }) }

    private fun loadSectionsByVenueSpaceId(venueSpaceId: UUID): Map<UUID, List<Pair<String, String>>> =
        jdbcTemplate.query(
            """
            select s.layout_template_id, s.section_key, s.label
            from layout_template_sections s
            join layout_templates lt on lt.id = s.layout_template_id
            where lt.venue_space_id = ?
            order by s.layout_template_id, s.sort_order, s.section_key
            """.trimIndent(),
            { rs, _ ->
                Triple(rs.uuid("layout_template_id"), rs.getString("section_key"), rs.getString("label"))
            },
            venueSpaceId
        ).groupBy({ it.first }, { it.second to it.third })

    private fun findSections(layoutTemplateId: UUID): List<Section> {
        val rowsBySectionKey = jdbcTemplate.query(
            """
            select section_key, row_key, label, start_seat, end_seat, price
            from layout_template_rows
            where layout_template_id = ?
            order by section_key, sort_order, row_key
            """.trimIndent(),
            { rs, _ -> rs.getString("section_key") to mapRow(rs) },
            layoutTemplateId
        ).groupBy({ it.first }, { it.second })

        return jdbcTemplate.query(
            """
            select section_key, label
            from layout_template_sections
            where layout_template_id = ?
            order by sort_order, section_key
            """.trimIndent(),
            { rs, _ ->
                val sectionKey = rs.getString("section_key")
                Section(
                    label = rs.getString("label"),
                    key = sectionKey,
                    rows = rowsBySectionKey[sectionKey].orEmpty()
                )
            },
            layoutTemplateId
        )
    }

    private fun mapRow(rs: java.sql.ResultSet): Row = Row(
        label = rs.getString("label"),
        key = rs.getString("row_key"),
        startSeat = rs.getInt("start_seat"),
        endSeat = rs.getInt("end_seat"),
        price = rs.getInt("price")
    )
}

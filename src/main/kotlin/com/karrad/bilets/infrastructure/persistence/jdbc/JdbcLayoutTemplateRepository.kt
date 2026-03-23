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
        val updated = jdbcTemplate.update(
            """
            update layout_templates
            set venue_space_id = ?, label = ?
            where id = ?
            """.trimIndent(),
            layoutTemplate.venueSpaceId,
            layoutTemplate.label,
            layoutTemplate.id
        )
        if (updated == 0) {
            jdbcTemplate.update(
                """
                insert into layout_templates (id, venue_space_id, label)
                values (?, ?, ?)
                """.trimIndent(),
                layoutTemplate.id,
                layoutTemplate.venueSpaceId,
                layoutTemplate.label
            )
        }

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

    override fun findAll(): List<LayoutTemplate> = jdbcTemplate.query(
        """
        select id, venue_space_id, label
        from layout_templates
        order by label, id
        """.trimIndent()
    ) { rs, _ ->
        val templateId = rs.uuid("id")
        LayoutTemplate(
            venueSpaceId = rs.uuid("venue_space_id"),
            label = rs.getString("label"),
            sections = findSections(templateId),
            id = templateId
        )
    }

    override fun findByVenueSpaceId(venueSpaceId: UUID): List<LayoutTemplate> = jdbcTemplate.query(
        """
        select id, venue_space_id, label
        from layout_templates
        where venue_space_id = ?
        order by label, id
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
        venueSpaceId
    )

    override fun deleteById(id: UUID): Boolean {
        jdbcTemplate.update("delete from layout_template_rows where layout_template_id = ?", id)
        jdbcTemplate.update("delete from layout_template_sections where layout_template_id = ?", id)
        return jdbcTemplate.update("delete from layout_templates where id = ?", id) > 0
    }

    private fun findSections(layoutTemplateId: UUID): List<Section> {
        val rowsBySectionKey = jdbcTemplate.query(
            """
            select section_key, row_key, label, start_seat, end_seat, price
            from layout_template_rows
            where layout_template_id = ?
            order by section_key, sort_order, row_key
            """.trimIndent(),
            { rs, _ ->
                rs.getString("section_key") to Row(
                    label = rs.getString("label"),
                    key = rs.getString("row_key"),
                    startSeat = rs.getInt("start_seat"),
                    endSeat = rs.getInt("end_seat"),
                    price = rs.getInt("price")
                )
            },
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
}

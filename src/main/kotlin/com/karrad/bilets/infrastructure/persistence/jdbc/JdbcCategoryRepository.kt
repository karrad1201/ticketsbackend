package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.domain.repository.CategoryRepository
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

class JdbcCategoryRepository(
    private val jdbcTemplate: JdbcTemplate
) : CategoryRepository {

    override fun save(category: Category): Category {
        val updated = jdbcTemplate.update(
            """
            update categories
            set code = ?, label = ?
            where id = ?
            """.trimIndent(),
            category.code,
            category.label,
            category.id
        )
        if (updated == 0) {
            jdbcTemplate.update(
                """
                insert into categories (id, code, label)
                values (?, ?, ?)
                """.trimIndent(),
                category.id,
                category.code,
                category.label
            )
        }
        return category
    }

    override fun findById(id: UUID): Category? = jdbcTemplate.query(
        """
        select id, code, label
        from categories
        where id = ?
        """.trimIndent(),
        { rs, _ ->
            Category(
                code = rs.getString("code"),
                label = rs.getString("label"),
                id = rs.uuid("id")
            )
        },
        id
    ).singleOrNull()

    override fun findByCode(code: String): Category? = jdbcTemplate.query(
        """
        select id, code, label
        from categories
        where code = ?
        """.trimIndent(),
        { rs, _ ->
            Category(
                code = rs.getString("code"),
                label = rs.getString("label"),
                id = rs.uuid("id")
            )
        },
        code
    ).singleOrNull()

    override fun findAll(): List<Category> = jdbcTemplate.query(
        """
        select id, code, label
        from categories
        order by label, id
        """.trimIndent()
    ) { rs, _ ->
        Category(
            code = rs.getString("code"),
            label = rs.getString("label"),
            id = rs.uuid("id")
        )
    }

    override fun deleteById(id: UUID): Boolean = jdbcTemplate.update(
        "delete from categories where id = ?",
        id
    ) > 0
}

package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.domain.repository.CategoryRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

class JdbcCategoryRepository(
    private val jdbcTemplate: JdbcTemplate
) : CategoryRepository {

    @CacheEvict(cacheNames = ["categories.all"], allEntries = true)
    override fun save(category: Category): Category {
        jdbcTemplate.update(
            """
            insert into categories (id, code, label)
            values (?, ?, ?)
            on conflict (id) do update set code = excluded.code, label = excluded.label
            """.trimIndent(),
            category.id, category.code, category.label
        )
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

    @Cacheable("categories.all")
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

    @CacheEvict(cacheNames = ["categories.all"], allEntries = true)
    override fun deleteById(id: UUID): Boolean = jdbcTemplate.update(
        "delete from categories where id = ?",
        id
    ) > 0
}

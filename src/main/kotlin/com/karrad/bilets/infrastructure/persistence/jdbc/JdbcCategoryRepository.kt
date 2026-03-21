package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.domain.repository.CategoryRepository
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JdbcCategoryRepository(
    private val jdbcClient: JdbcClient
) : CategoryRepository {

    override fun save(category: Category): Category {
        jdbcClient.sql(
            """
            merge into categories (id, code, label)
            key (id)
            values (:id, :code, :label)
            """.trimIndent()
        )
            .param("id", category.id)
            .param("code", category.code)
            .param("label", category.label)
            .update()

        return category
    }

    override fun findById(id: UUID): Category? {
        return jdbcClient.sql(
            """
            select id, code, label
            from categories
            where id = :id
            """.trimIndent()
        )
            .param("id", id)
            .query(::mapRow)
            .optional()
            .orElse(null)
    }

    override fun findByCode(code: String): Category? {
        return jdbcClient.sql(
            """
            select id, code, label
            from categories
            where code = :code
            """.trimIndent()
        )
            .param("code", code)
            .query(::mapRow)
            .optional()
            .orElse(null)
    }

    override fun findAll(): List<Category> {
        return jdbcClient.sql(
            """
            select id, code, label
            from categories
            order by code
            """.trimIndent()
        )
            .query(::mapRow)
            .list()
    }

    override fun deleteById(id: UUID): Boolean {
        val updatedRows = jdbcClient.sql(
            """
            delete from categories
            where id = :id
            """.trimIndent()
        )
            .param("id", id)
            .update()

        return updatedRows > 0
    }

    private fun mapRow(rs: java.sql.ResultSet, rowNum: Int): Category {
        return Category(
            id = rs.getObject("id", UUID::class.java),
            code = rs.getString("code"),
            label = rs.getString("label")
        )
    }
}

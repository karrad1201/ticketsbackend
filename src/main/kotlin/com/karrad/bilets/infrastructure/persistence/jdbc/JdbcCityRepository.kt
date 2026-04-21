package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.repository.CityRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.jdbc.core.JdbcTemplate

class JdbcCityRepository(
    private val jdbcTemplate: JdbcTemplate
) : CityRepository {

    @Cacheable("cities.all")
    override fun findAll(): List<City> = jdbcTemplate.query(
        """
        SELECT c.id, c.label, s.id as subject_id, s.label as subject_label
        FROM cities c
        JOIN subjects s ON c.subject_id = s.id
        ORDER BY c.label
        """.trimIndent()
    ) { rs, _ ->
        City(
            id = rs.uuid("id"),
            label = rs.getString("label"),
            subject = Subject(
                id = rs.uuid("subject_id"),
                label = rs.getString("subject_label")
            )
        )
    }
}

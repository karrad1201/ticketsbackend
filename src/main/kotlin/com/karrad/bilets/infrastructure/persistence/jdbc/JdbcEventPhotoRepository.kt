package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.EventPhoto
import com.karrad.bilets.domain.repository.EventPhotoRepository
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.UUID

class JdbcEventPhotoRepository(
    private val jdbcTemplate: JdbcTemplate
) : EventPhotoRepository {

    override fun save(photo: EventPhoto): EventPhoto {
        jdbcTemplate.update(
            """
            insert into event_photos (id, event_id, url, sort_order, uploaded_at)
            values (?, ?, ?, ?, ?)
            on conflict (id) do update set url = excluded.url, sort_order = excluded.sort_order
            """.trimIndent(),
            photo.id, photo.eventId, photo.url, photo.sortOrder,
            Timestamp.from(photo.uploadedAt)
        )
        return photo
    }

    override fun findByEventId(eventId: UUID): List<EventPhoto> =
        jdbcTemplate.query(
            "select * from event_photos where event_id = ? order by sort_order, uploaded_at",
            { rs, _ -> rs.toEventPhoto() },
            eventId
        )

    override fun deleteById(id: UUID): Boolean =
        jdbcTemplate.update("delete from event_photos where id = ?", id) > 0

    private fun ResultSet.toEventPhoto() = EventPhoto(
        id = getObject("id", UUID::class.java),
        eventId = getObject("event_id", UUID::class.java),
        url = getString("url"),
        sortOrder = getInt("sort_order"),
        uploadedAt = getTimestamp("uploaded_at").toInstant()
    )
}

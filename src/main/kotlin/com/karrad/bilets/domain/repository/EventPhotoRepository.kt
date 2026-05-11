package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.EventPhoto
import java.util.UUID

interface EventPhotoRepository {
    fun save(photo: EventPhoto): EventPhoto
    fun findByEventId(eventId: UUID): List<EventPhoto>
    fun deleteById(id: UUID): Boolean
}

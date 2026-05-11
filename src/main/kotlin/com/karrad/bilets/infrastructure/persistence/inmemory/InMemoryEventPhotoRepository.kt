package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.EventPhoto
import com.karrad.bilets.domain.repository.EventPhotoRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryEventPhotoRepository : EventPhotoRepository {
    private val storage = ConcurrentHashMap<UUID, EventPhoto>()

    override fun save(photo: EventPhoto): EventPhoto {
        storage[photo.id] = photo
        return photo
    }

    override fun findByEventId(eventId: UUID): List<EventPhoto> =
        storage.values.filter { it.eventId == eventId }.sortedWith(compareBy({ it.sortOrder }, { it.uploadedAt }))

    override fun deleteById(id: UUID): Boolean = storage.remove(id) != null
}

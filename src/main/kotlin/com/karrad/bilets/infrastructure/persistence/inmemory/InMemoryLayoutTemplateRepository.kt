package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.LayoutTemplate
import com.karrad.bilets.domain.repository.LayoutTemplateRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryLayoutTemplateRepository : LayoutTemplateRepository {
    private val storage = ConcurrentHashMap<UUID, LayoutTemplate>()

    override fun save(layoutTemplate: LayoutTemplate): LayoutTemplate {
        storage[layoutTemplate.id] = layoutTemplate
        return layoutTemplate
    }

    override fun findById(id: UUID): LayoutTemplate? = storage[id]

    override fun findAll(): List<LayoutTemplate> = storage.values.toList()

    override fun findByVenueSpaceId(venueSpaceId: UUID): List<LayoutTemplate> =
        storage.values.filter { it.venueSpaceId == venueSpaceId }

    override fun deleteById(id: UUID): Boolean = storage.remove(id) != null
}

package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.LayoutTemplate
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import java.util.UUID

interface LayoutTemplateRepository {
    @CacheEvict(cacheNames = ["layoutTemplates.byVenueSpaceId"], allEntries = true)
    fun save(layoutTemplate: LayoutTemplate): LayoutTemplate
    fun findById(id: UUID): LayoutTemplate?
    fun findAll(): List<LayoutTemplate>
    @Cacheable(value = ["layoutTemplates.byVenueSpaceId"], key = "#venueSpaceId")
    fun findByVenueSpaceId(venueSpaceId: UUID): List<LayoutTemplate>
    @CacheEvict(cacheNames = ["layoutTemplates.byVenueSpaceId"], allEntries = true)
    fun deleteById(id: UUID): Boolean
}

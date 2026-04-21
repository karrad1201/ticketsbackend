package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.LayoutTemplate
import java.util.UUID

interface LayoutTemplateRepository {
    fun save(layoutTemplate: LayoutTemplate): LayoutTemplate
    fun findById(id: UUID): LayoutTemplate?
    fun findAll(): List<LayoutTemplate>
    fun findByVenueSpaceId(venueSpaceId: UUID): List<LayoutTemplate>
    fun deleteById(id: UUID): Boolean
}

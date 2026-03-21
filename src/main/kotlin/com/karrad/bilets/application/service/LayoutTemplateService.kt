package com.karrad.bilets.application.service

import com.karrad.bilets.domain.entity.LayoutTemplate
import com.karrad.bilets.domain.repository.LayoutTemplateRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class LayoutTemplateService(
    private val layoutTemplateRepository: LayoutTemplateRepository
) {
    fun create(layoutTemplate: LayoutTemplate): LayoutTemplate = layoutTemplateRepository.save(layoutTemplate)

    fun getById(id: UUID): LayoutTemplate? = layoutTemplateRepository.findById(id)

    fun list(): List<LayoutTemplate> = layoutTemplateRepository.findAll()

    fun listByVenueSpaceId(venueSpaceId: UUID): List<LayoutTemplate> =
        layoutTemplateRepository.findByVenueSpaceId(venueSpaceId)

    fun update(layoutTemplate: LayoutTemplate): LayoutTemplate {
        requireNotNull(layoutTemplateRepository.findById(layoutTemplate.id)) {
            "LayoutTemplate not found: ${layoutTemplate.id}"
        }
        return layoutTemplateRepository.save(layoutTemplate)
    }

    fun deleteById(id: UUID): Boolean = layoutTemplateRepository.deleteById(id)
}

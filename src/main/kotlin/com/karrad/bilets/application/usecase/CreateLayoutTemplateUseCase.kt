package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.LayoutTemplate
import com.karrad.bilets.domain.repository.LayoutTemplateRepository
import com.karrad.bilets.domain.repository.VenueRepository
import org.springframework.stereotype.Component

@Component
class CreateLayoutTemplateUseCase(
    private val venueRepository: VenueRepository,
    private val layoutTemplateRepository: LayoutTemplateRepository
) {
    fun create(layoutTemplate: LayoutTemplate): LayoutTemplate {
        requireNotNull(venueRepository.findBySpaceId(layoutTemplate.venueSpaceId)) {
            "VenueSpace not found: ${layoutTemplate.venueSpaceId}"
        }

        return layoutTemplateRepository.save(layoutTemplate)
    }
}

package com.karrad.bilets.web

import com.karrad.bilets.application.service.LayoutTemplateService
import com.karrad.bilets.application.usecase.CreateLayoutTemplateUseCase
import com.karrad.bilets.domain.entity.LayoutTemplate
import com.karrad.bilets.web.dto.CreateLayoutTemplateRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/layout-templates")
class LayoutTemplateController(
    private val createLayoutTemplateUseCase: CreateLayoutTemplateUseCase,
    private val layoutTemplateService: LayoutTemplateService,
    private val currentUserProvider: CurrentUserProvider
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody request: CreateLayoutTemplateRequest
    ): LayoutTemplate {
        return createLayoutTemplateUseCase.create(request.toDomain(), currentUserProvider.requireUserId())
    }

    @GetMapping
    fun list(@RequestParam(required = false) venueSpaceId: java.util.UUID?): List<LayoutTemplate> {
        return if (venueSpaceId == null) layoutTemplateService.list() else layoutTemplateService.listByVenueSpaceId(venueSpaceId)
    }

    @GetMapping("/{layoutTemplateId}")
    fun getById(@PathVariable layoutTemplateId: java.util.UUID): LayoutTemplate =
        layoutTemplateService.getById(layoutTemplateId) ?: throw NoSuchElementException("LayoutTemplate not found: $layoutTemplateId")
}

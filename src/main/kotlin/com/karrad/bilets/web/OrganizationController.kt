package com.karrad.bilets.web

import com.karrad.bilets.application.service.OrganizationService
import com.karrad.bilets.application.usecase.CreateOrganizationUseCase
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.web.dto.CreateOrganizationRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/organizations")
class OrganizationController(
    private val createOrganizationUseCase: CreateOrganizationUseCase,
    private val organizationService: OrganizationService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateOrganizationRequest): Organization {
        return createOrganizationUseCase.create(request.toDomain())
    }

    @GetMapping
    fun list(): List<Organization> = organizationService.list()

    @GetMapping("/{organizationId}")
    fun getById(@PathVariable organizationId: UUID): Organization =
        organizationService.getById(organizationId)
            ?: throw NoSuchElementException("Organization not found: $organizationId")
}

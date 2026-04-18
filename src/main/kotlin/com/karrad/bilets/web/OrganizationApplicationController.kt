package com.karrad.bilets.web

import com.karrad.bilets.application.service.OrganizationApplicationService
import com.karrad.bilets.application.usecase.ReviewOrganizationApplicationUseCase
import com.karrad.bilets.application.usecase.SubmitOrganizationApplicationUseCase
import com.karrad.bilets.domain.entity.OrganizationApplication
import com.karrad.bilets.web.dto.CreateOrganizationApplicationRequest
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
@RequestMapping("/api/v1/organization-applications")
class OrganizationApplicationController(
    private val submitOrganizationApplicationUseCase: SubmitOrganizationApplicationUseCase,
    private val reviewOrganizationApplicationUseCase: ReviewOrganizationApplicationUseCase,
    private val organizationApplicationService: OrganizationApplicationService,
    private val currentUserProvider: CurrentUserProvider
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateOrganizationApplicationRequest): OrganizationApplication {
        return submitOrganizationApplicationUseCase.submit(request.toDomain(currentUserProvider.requireUserId()))
    }

    @GetMapping
    fun list(): List<OrganizationApplication> = organizationApplicationService.list()

    @GetMapping("/{applicationId}")
    fun getById(@PathVariable applicationId: UUID): OrganizationApplication =
        organizationApplicationService.getById(applicationId)
            ?: throw NoSuchElementException("OrganizationApplication not found: $applicationId")

    @PostMapping("/{applicationId}/approve")
    fun approve(@PathVariable applicationId: UUID): OrganizationApplication =
        reviewOrganizationApplicationUseCase.approve(
            applicationId = applicationId,
            adminUserId = currentUserProvider.requireAdmin().id
        )

    @PostMapping("/{applicationId}/reject")
    fun reject(@PathVariable applicationId: UUID): OrganizationApplication =
        reviewOrganizationApplicationUseCase.reject(
            applicationId = applicationId,
            adminUserId = currentUserProvider.requireAdmin().id
        )
}

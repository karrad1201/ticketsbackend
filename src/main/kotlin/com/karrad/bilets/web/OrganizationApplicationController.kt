package com.karrad.bilets.web

import com.karrad.bilets.application.service.OrganizationApplicationService
import com.karrad.bilets.application.usecase.ReviewOrganizationApplicationUseCase
import com.karrad.bilets.application.usecase.SubmitOrganizationApplicationUseCase
import com.karrad.bilets.domain.entity.OrganizationApplication
import com.karrad.bilets.web.dto.CreateOrganizationApplicationRequest
import com.karrad.bilets.web.dto.ReviewOrganizationApplicationRequest
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
@RequestMapping("/api/organization-applications")
class OrganizationApplicationController(
    private val submitOrganizationApplicationUseCase: SubmitOrganizationApplicationUseCase,
    private val reviewOrganizationApplicationUseCase: ReviewOrganizationApplicationUseCase,
    private val organizationApplicationService: OrganizationApplicationService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateOrganizationApplicationRequest): OrganizationApplication {
        return submitOrganizationApplicationUseCase.submit(request.toDomain())
    }

    @GetMapping
    fun list(): List<OrganizationApplication> = organizationApplicationService.list()

    @GetMapping("/{applicationId}")
    fun getById(@PathVariable applicationId: UUID): OrganizationApplication =
        organizationApplicationService.getById(applicationId)
            ?: throw NoSuchElementException("OrganizationApplication not found: $applicationId")

    @PostMapping("/{applicationId}/approve")
    fun approve(
        @PathVariable applicationId: UUID,
        @RequestBody request: ReviewOrganizationApplicationRequest
    ): OrganizationApplication {
        return reviewOrganizationApplicationUseCase.approve(applicationId = applicationId, adminUserId = request.adminUserId)
    }

    @PostMapping("/{applicationId}/reject")
    fun reject(
        @PathVariable applicationId: UUID,
        @RequestBody request: ReviewOrganizationApplicationRequest
    ): OrganizationApplication {
        return reviewOrganizationApplicationUseCase.reject(applicationId = applicationId, adminUserId = request.adminUserId)
    }
}

package com.karrad.bilets.web

import com.karrad.bilets.application.service.OrganizationApplicationService
import com.karrad.bilets.application.usecase.ReviewOrganizationApplicationUseCase
import com.karrad.bilets.application.usecase.SubmitOrganizationApplicationUseCase
import com.karrad.bilets.domain.entity.OrganizationApplication
import com.karrad.bilets.domain.enums.OrganizationApplicationStatus
import com.karrad.bilets.domain.repository.OrganizationApplicationRepository
import com.karrad.bilets.web.dto.CreateOrganizationApplicationRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.util.UUID

@RestController
@RequestMapping("/api/v1/organization-applications")
class OrganizationApplicationController(
    private val submitOrganizationApplicationUseCase: SubmitOrganizationApplicationUseCase,
    private val reviewOrganizationApplicationUseCase: ReviewOrganizationApplicationUseCase,
    private val organizationApplicationService: OrganizationApplicationService,
    private val organizationApplicationRepository: OrganizationApplicationRepository,
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

    /** Загрузить документ к заявке (владелец заявки). */
    @PostMapping(
        "/{applicationId}/documents",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    fun uploadDocument(
        @PathVariable applicationId: UUID,
        @RequestParam("file") file: MultipartFile
    ): OrganizationApplication {
        require(!file.isEmpty) { "File must not be empty" }
        require(file.size <= 10 * 1024 * 1024) { "File size must not exceed 10 MB" }

        val callerId = currentUserProvider.requireUserId()
        val application = organizationApplicationRepository.findById(applicationId)
            ?: throw NoSuchElementException("OrganizationApplication not found: $applicationId")
        require(application.applicantUserId == callerId) {
            "OrganizationApplication does not belong to you"
        }
        require(application.status == OrganizationApplicationStatus.PENDING) {
            "Cannot add documents to a non-pending application"
        }

        val uploadsDir = File("uploads/org-docs")
        uploadsDir.mkdirs()
        val filename = "${UUID.randomUUID()}_${file.originalFilename?.replace(" ", "_") ?: "doc"}"
        val dest = File(uploadsDir, filename)
        file.transferTo(dest)

        val url = "/uploads/org-docs/$filename"
        return organizationApplicationRepository.save(application.addDocument(url))
    }
}

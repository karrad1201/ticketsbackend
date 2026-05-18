package com.karrad.bilets.web

import com.karrad.bilets.application.usecase.ReviewVenueApplicationUseCase
import com.karrad.bilets.application.usecase.SubmitVenueApplicationUseCase
import com.karrad.bilets.domain.entity.VenueApplication
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.enums.VenueApplicationStatus
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.VenueApplicationRepository
import org.springframework.beans.factory.annotation.Value
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
import java.time.Clock
import java.util.UUID

@RestController
class VenueApplicationController(
    private val submitVenueApplicationUseCase: SubmitVenueApplicationUseCase,
    private val reviewVenueApplicationUseCase: ReviewVenueApplicationUseCase,
    private val venueApplicationRepository: VenueApplicationRepository,
    private val organizationMemberRepository: OrganizationMemberRepository,
    private val currentUserProvider: CurrentUserProvider,
    private val clock: Clock,
    @Value("\${app.uploads.dir}") private val uploadsDir: String
) {

    /** Подать заявку на создание площадки (OWNER текущей организации). */
    @PostMapping("/api/v1/my/organization/venue-applications")
    @ResponseStatus(HttpStatus.CREATED)
    fun submit(@RequestBody body: CreateVenueApplicationRequest): VenueApplication {
        val callerId = currentUserProvider.requireUserId()
        val membership = organizationMemberRepository.findByUserId(callerId).firstOrNull()
            ?: throw SecurityException("Not a member of any organization")
        require(membership.role == OrganizationMemberRole.OWNER) {
            "Only OWNER can submit venue applications"
        }
        return submitVenueApplicationUseCase.submit(
            VenueApplication(
                organizationId = membership.organizationId,
                applicantUserId = callerId,
                name = body.name,
                cityLabel = body.cityLabel,
                subjectLabel = body.subjectLabel,
                address = body.address,
                description = body.description,
                createdAt = clock.instant()
            )
        )
    }

    /** Список заявок организации текущего пользователя. */
    @GetMapping("/api/v1/my/organization/venue-applications")
    fun listMine(): List<VenueApplication> {
        val callerId = currentUserProvider.requireUserId()
        val membership = organizationMemberRepository.findByUserId(callerId).firstOrNull()
            ?: throw SecurityException("Not a member of any organization")
        return venueApplicationRepository.findByOrganizationId(membership.organizationId)
    }

    /** Загрузить документ к заявке (OWNER, только свои заявки). */
    @PostMapping(
        "/api/v1/my/organization/venue-applications/{applicationId}/documents",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    fun uploadDocument(
        @PathVariable applicationId: UUID,
        @RequestParam("file") file: MultipartFile
    ): VenueApplication {
        require(!file.isEmpty) { "File must not be empty" }
        require((file.size) <= 10 * 1024 * 1024) { "File size must not exceed 10 MB" }

        val callerId = currentUserProvider.requireUserId()
        val membership = organizationMemberRepository.findByUserId(callerId).firstOrNull()
            ?: throw SecurityException("Not a member of any organization")

        val application = venueApplicationRepository.findById(applicationId)
            ?: throw NoSuchElementException("VenueApplication not found: $applicationId")

        require(application.organizationId == membership.organizationId) {
            "VenueApplication does not belong to your organization"
        }
        require(application.status == VenueApplicationStatus.PENDING) {
            "Cannot add documents to a non-pending application"
        }

        val dir = File("$uploadsDir/venue-docs")
        dir.mkdirs()
        val filename = "${UUID.randomUUID()}_${file.originalFilename?.replace(" ", "_") ?: "doc"}"
        val dest = File(dir, filename)
        file.transferTo(dest)

        val url = "/uploads/venue-docs/$filename"
        return venueApplicationRepository.save(application.addDocument(url))
    }

    /** Список всех заявок (ADMIN). Фильтр по статусу: ?status=PENDING */
    @GetMapping("/api/v1/venue-applications")
    fun listAll(@RequestParam(required = false) status: VenueApplicationStatus?): List<VenueApplication> {
        currentUserProvider.requireAdmin()
        return if (status != null) {
            venueApplicationRepository.findByStatus(status)
        } else {
            venueApplicationRepository.findAll()
        }
    }

    /** Получить заявку по ID (ADMIN). */
    @GetMapping("/api/v1/venue-applications/{applicationId}")
    fun getById(@PathVariable applicationId: UUID): VenueApplication {
        currentUserProvider.requireAdmin()
        return venueApplicationRepository.findById(applicationId)
            ?: throw NoSuchElementException("VenueApplication not found: $applicationId")
    }

    /** Одобрить заявку (ADMIN). */
    @PostMapping("/api/v1/venue-applications/{applicationId}/approve")
    fun approve(@PathVariable applicationId: UUID): VenueApplication =
        reviewVenueApplicationUseCase.approve(
            applicationId = applicationId,
            adminUserId = currentUserProvider.requireAdmin().id
        )

    /** Отклонить заявку (ADMIN). */
    @PostMapping("/api/v1/venue-applications/{applicationId}/reject")
    fun reject(@PathVariable applicationId: UUID): VenueApplication =
        reviewVenueApplicationUseCase.reject(
            applicationId = applicationId,
            adminUserId = currentUserProvider.requireAdmin().id
        )
}

data class CreateVenueApplicationRequest(
    val name: String,
    val cityLabel: String,
    val subjectLabel: String,
    val address: String,
    val description: String? = null
)

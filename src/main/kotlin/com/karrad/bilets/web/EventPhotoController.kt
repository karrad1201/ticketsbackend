package com.karrad.bilets.web

import com.karrad.bilets.application.service.EventService
import com.karrad.bilets.domain.entity.EventPhoto
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.EventPhotoRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.util.UUID

@RestController
@RequestMapping("/api/v1/events/{eventId}/photos")
class EventPhotoController(
    private val eventPhotoRepository: EventPhotoRepository,
    private val eventService: EventService,
    private val organizationMemberRepository: OrganizationMemberRepository,
    private val userRepository: UserRepository,
    private val currentUserProvider: CurrentUserProvider
) {

    @GetMapping
    fun list(@PathVariable eventId: UUID): List<EventPhoto> =
        eventPhotoRepository.findByEventId(eventId)

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun upload(
        @PathVariable eventId: UUID,
        @RequestParam("file") file: MultipartFile,
        @RequestParam(defaultValue = "0") sortOrder: Int
    ): EventPhoto {
        val callerId = currentUserProvider.requireUserId()
        requireOrgPermission(callerId, eventId)

        require(!file.isEmpty) { "File must not be empty" }
        require(file.size <= MAX_FILE_SIZE) { "Photo file must not exceed 10 MB" }
        val ext = requireNotNull(ALLOWED_TYPES[file.contentType ?: ""]) { "Photo must be JPEG, PNG or WebP" }

        val photoId = UUID.randomUUID()
        val dir = File("uploads/events/$eventId/photos")
        dir.mkdirs()
        val dest = File(dir, "$photoId.$ext")
        file.transferTo(dest)

        val photo = EventPhoto(
            id = photoId,
            eventId = eventId,
            url = "/uploads/events/$eventId/photos/$photoId.$ext",
            sortOrder = sortOrder
        )
        return eventPhotoRepository.save(photo)
    }

    @DeleteMapping("/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable eventId: UUID, @PathVariable photoId: UUID) {
        val callerId = currentUserProvider.requireUserId()
        requireOrgPermission(callerId, eventId)
        eventPhotoRepository.deleteById(photoId)
    }

    private fun requireOrgPermission(callerId: UUID, eventId: UUID) {
        val event = requireNotNull(eventService.getById(eventId)) { "Event not found: $eventId" }
        val user = userRepository.findById(callerId)
        if (user?.role == UserRole.ADMIN) return
        requireNotNull(event.organizationId) { "Event is not attached to any organization" }
        val membership = requireNotNull(
            organizationMemberRepository.findByOrganizationIdAndUserId(event.organizationId!!, callerId)
        ) { "Not a member of the event's organization" }
        require(membership.role in setOf(OrganizationMemberRole.OWNER, OrganizationMemberRole.MANAGER)) {
            "Insufficient role: ${membership.role}"
        }
    }

    companion object {
        const val MAX_FILE_SIZE = 10L * 1024 * 1024
        val ALLOWED_TYPES = mapOf(
            "image/jpeg" to "jpg",
            "image/png" to "png",
            "image/webp" to "webp"
        )
    }
}

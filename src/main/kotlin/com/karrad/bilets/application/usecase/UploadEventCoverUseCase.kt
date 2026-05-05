package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.util.UUID

@Component
class UploadEventCoverUseCase(
    private val eventRepository: EventRepository,
    private val organizationMemberRepository: OrganizationMemberRepository
) {
    fun upload(eventId: UUID, file: MultipartFile, callerId: UUID): Event {
        require(!file.isEmpty) { "File must not be empty" }
        require(file.size <= MAX_FILE_SIZE) { "Cover file must not exceed 10 MB" }

        val contentType = file.contentType ?: ""
        require(contentType in ALLOWED_TYPES) {
            "Cover must be JPEG, PNG or WebP image"
        }

        val event = requireNotNull(eventRepository.findById(eventId)) {
            "Event not found: $eventId"
        }

        val membership = organizationMemberRepository.findByUserId(callerId).firstOrNull()
            ?: throw SecurityException("User $callerId is not a member of any organization")

        require(membership.organizationId == event.organizationId) {
            "Only the event's organization can upload a cover"
        }

        val ext = ALLOWED_TYPES[contentType]!!
        val dir = File("uploads/events/$eventId")
        dir.mkdirs()
        val dest = File(dir, "cover.$ext")
        file.transferTo(dest)

        val imageUrl = "/uploads/events/$eventId/cover.$ext"
        return eventRepository.save(event.copy(imageUrl = imageUrl))
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

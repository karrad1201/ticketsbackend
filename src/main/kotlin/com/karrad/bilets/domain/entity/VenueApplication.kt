package com.karrad.bilets.domain.entity

import com.karrad.bilets.domain.enums.VenueApplicationStatus
import java.time.Instant
import java.util.UUID

data class VenueApplication(
    val organizationId: UUID,
    val applicantUserId: UUID,
    val name: String,
    val cityLabel: String,
    val subjectLabel: String,
    val address: String,
    val description: String? = null,
    val documentUrls: List<String> = emptyList(),
    val status: VenueApplicationStatus = VenueApplicationStatus.PENDING,
    val reviewedByUserId: UUID? = null,
    val reviewedAt: Instant? = null,
    val venueId: UUID? = null,
    val createdAt: Instant,
    val id: UUID = UUID.randomUUID()
) {
    init {
        require(name.isNotBlank()) { "VenueApplication name must not be blank" }
        require(cityLabel.isNotBlank()) { "VenueApplication cityLabel must not be blank" }
        require(subjectLabel.isNotBlank()) { "VenueApplication subjectLabel must not be blank" }
        require(address.isNotBlank()) { "VenueApplication address must not be blank" }
        require(documentUrls.size <= 10) { "VenueApplication documentUrls must not exceed 10" }
    }

    fun approve(adminUserId: UUID, approvedVenueId: UUID, at: Instant): VenueApplication {
        check(status == VenueApplicationStatus.PENDING) { "Only pending application can be approved" }
        return copy(
            status = VenueApplicationStatus.APPROVED,
            reviewedByUserId = adminUserId,
            reviewedAt = at,
            venueId = approvedVenueId
        )
    }

    fun reject(adminUserId: UUID, at: Instant): VenueApplication {
        check(status == VenueApplicationStatus.PENDING) { "Only pending application can be rejected" }
        return copy(
            status = VenueApplicationStatus.REJECTED,
            reviewedByUserId = adminUserId,
            reviewedAt = at
        )
    }

    fun addDocument(url: String): VenueApplication {
        require(documentUrls.size < 10) { "Cannot add more than 10 documents" }
        return copy(documentUrls = documentUrls + url)
    }
}

package com.karrad.bilets.domain.entity

import java.time.Instant
import java.util.UUID


data class Event(
    val label: String,
    val description: String,
    val venueId: UUID,
    val categoryId: UUID,
    val time: Instant,
    val venueSpaceId: UUID? = null,
    val id: UUID = UUID.randomUUID(),
    val organizationId: UUID? = null,
    val salesClosedAt: Instant? = null,
    val imageUrl: String? = null,
    val minPrice: Int? = null,
    val ageRating: String? = null
) {
    init {
        require(label.isNotBlank()) { "Event label must not be blank" }
        require(description.isNotBlank()) { "Event description must not be blank" }
    }

    fun isSalesClosed(now: Instant): Boolean = salesClosedAt != null || !time.isAfter(now)

    fun closeSales(closedAt: Instant): Event {
        if (salesClosedAt != null) {
            return this
        }
        return copy(salesClosedAt = closedAt)
    }
}

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
    val ageRating: String? = null,
    val hasSeatMap: Boolean = false
) {
    init {
        require(label.isNotBlank()) { "Event label must not be blank" }
        require(description.isNotBlank()) { "Event description must not be blank" }
        if (ageRating != null) {
            require(ageRating in ALLOWED_AGE_RATINGS) {
                "Event ageRating must be one of $ALLOWED_AGE_RATINGS but was '$ageRating'"
            }
        }
    }

    companion object {
        val ALLOWED_AGE_RATINGS = setOf("0+", "6+", "12+", "16+", "18+")
    }

    fun isSalesClosed(now: Instant): Boolean = salesClosedAt != null || !time.isAfter(now)

    fun closeSales(closedAt: Instant): Event {
        if (salesClosedAt != null) {
            return this
        }
        return copy(salesClosedAt = closedAt)
    }
}

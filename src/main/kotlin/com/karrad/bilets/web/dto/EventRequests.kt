package com.karrad.bilets.web.dto

import com.karrad.bilets.domain.entity.Event
import java.time.Instant
import java.util.UUID

data class UpdateEventRequest(
    val label: String? = null,
    val description: String? = null,
    val time: String? = null,
    val ageRating: String? = null
)

data class CreateEventRequest(
    val label: String,
    val description: String,
    val venueId: UUID,
    val categoryId: UUID,
    val time: Instant,
    val venueSpaceId: UUID? = null,
    val imageUrl: String? = null,
    val ageRating: String? = null,
    val hasSeatMap: Boolean? = null,
    /** Optional: create multiple sessions. If provided and non-empty, overrides `time`. Max 10 entries. */
    val sessionTimes: List<Instant>? = null,
    /** Optional: auto-generate inventory from this price profile at creation time. */
    val priceProfileId: UUID? = null
) {
    fun toDomain(): Event {
        val effectiveTime = sessionTimes?.firstOrNull() ?: time
        return Event(
            label = label,
            description = description,
            venueId = venueId,
            categoryId = categoryId,
            time = effectiveTime,
            venueSpaceId = venueSpaceId,
            imageUrl = imageUrl,
            ageRating = ageRating,
            hasSeatMap = hasSeatMap ?: false
        )
    }

    fun effectiveSessionTimes(): List<Instant> =
        if (sessionTimes != null && sessionTimes.isNotEmpty()) sessionTimes.take(10)
        else listOf(time)
}

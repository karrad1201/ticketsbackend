package com.karrad.bilets.web.dto

import com.karrad.bilets.domain.entity.Event
import java.time.Instant
import java.util.UUID

data class CreateEventRequest(
    val label: String,
    val description: String,
    val venueId: UUID,
    val categoryId: UUID,
    val time: Instant,
    val venueSpaceId: UUID? = null,
    val imageUrl: String? = null,
    val ageRating: String? = null,
    val hasSeatMap: Boolean? = null
) {
    fun toDomain(): Event {
        return Event(
            label = label,
            description = description,
            venueId = venueId,
            categoryId = categoryId,
            time = time,
            venueSpaceId = venueSpaceId,
            imageUrl = imageUrl,
            ageRating = ageRating,
            hasSeatMap = hasSeatMap ?: false
        )
    }
}

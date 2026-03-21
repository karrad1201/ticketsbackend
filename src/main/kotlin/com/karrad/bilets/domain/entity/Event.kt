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
    val organizationId: UUID? = null
) {
    init {
        require(label.isNotBlank()) { "Event label must not be blank" }
        require(description.isNotBlank()) { "Event description must not be blank" }
    }
}

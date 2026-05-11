package com.karrad.bilets.domain.entity

import java.time.Instant
import java.util.UUID

data class EventPhoto(
    val eventId: UUID,
    val url: String,
    val sortOrder: Int = 0,
    val id: UUID = UUID.randomUUID(),
    val uploadedAt: Instant = Instant.now()
)

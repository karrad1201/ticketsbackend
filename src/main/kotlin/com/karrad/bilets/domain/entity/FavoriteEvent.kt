package com.karrad.bilets.domain.entity

import java.time.Instant
import java.util.UUID

data class FavoriteEvent(
    val userId: UUID,
    val eventId: UUID,
    val createdAt: Instant = Instant.now(),
    val id: UUID = UUID.randomUUID()
)

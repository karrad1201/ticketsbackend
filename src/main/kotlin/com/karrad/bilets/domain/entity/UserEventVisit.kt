package com.karrad.bilets.domain.entity

import java.time.Instant
import java.util.UUID

data class UserEventVisit(
    val userId: UUID,
    val eventId: UUID,
    val visitedAt: Instant,
    val id: UUID = UUID.randomUUID()
)

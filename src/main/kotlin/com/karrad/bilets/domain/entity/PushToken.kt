package com.karrad.bilets.domain.entity

import java.time.Instant
import java.util.UUID

data class PushToken(
    val id: UUID = UUID.randomUUID(),
    val userId: UUID,
    val token: String,
    val platform: String, // "android" | "ios"
    val createdAt: Instant = Instant.now()
)

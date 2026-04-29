package com.karrad.bilets.domain.entity

import java.time.Instant
import java.util.UUID

data class AdminCredential(
    val userId: UUID,
    val passwordHash: String,
    val id: UUID = UUID.randomUUID(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

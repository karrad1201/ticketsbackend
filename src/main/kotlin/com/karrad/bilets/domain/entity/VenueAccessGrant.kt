package com.karrad.bilets.domain.entity

import com.karrad.bilets.domain.enums.VenueAccessGrantStatus
import java.time.Instant
import java.util.UUID

data class VenueAccessGrant(
    val venueId: UUID,
    val requestingOrgId: UUID,
    val status: VenueAccessGrantStatus = VenueAccessGrantStatus.PENDING,
    val id: UUID = UUID.randomUUID(),
    val createdAt: Instant,
    val decidedAt: Instant? = null,
    val decidedBy: UUID? = null
)

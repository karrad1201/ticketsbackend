package com.karrad.bilets.web.dto

import com.karrad.bilets.domain.entity.VenueAccessGrant
import java.time.Instant
import java.util.UUID

data class RequestVenueAccessRequest(
    val requestingOrgId: UUID
)

data class VenueAccessGrantResponse(
    val id: UUID,
    val venueId: UUID,
    val requestingOrgId: UUID,
    val status: String,
    val createdAt: Instant,
    val decidedAt: Instant?,
    val decidedBy: UUID?
) {
    companion object {
        fun from(grant: VenueAccessGrant) = VenueAccessGrantResponse(
            id = grant.id,
            venueId = grant.venueId,
            requestingOrgId = grant.requestingOrgId,
            status = grant.status.name,
            createdAt = grant.createdAt,
            decidedAt = grant.decidedAt,
            decidedBy = grant.decidedBy
        )
    }
}

package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.VenueAccessGrant
import com.karrad.bilets.domain.enums.VenueAccessGrantStatus
import java.util.UUID

interface VenueAccessGrantRepository {
    fun save(grant: VenueAccessGrant): VenueAccessGrant
    fun findById(id: UUID): VenueAccessGrant?
    fun findByVenueId(venueId: UUID): List<VenueAccessGrant>
    fun findByVenueIdAndStatus(venueId: UUID, status: VenueAccessGrantStatus): List<VenueAccessGrant>
    fun findApprovedByVenueIdAndOrgId(venueId: UUID, orgId: UUID): VenueAccessGrant?
}

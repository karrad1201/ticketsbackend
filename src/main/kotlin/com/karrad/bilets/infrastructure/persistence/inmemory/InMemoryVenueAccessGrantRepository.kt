package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.VenueAccessGrant
import com.karrad.bilets.domain.enums.VenueAccessGrantStatus
import com.karrad.bilets.domain.repository.VenueAccessGrantRepository
import java.util.UUID

class InMemoryVenueAccessGrantRepository : VenueAccessGrantRepository {
    private val storage = linkedMapOf<UUID, VenueAccessGrant>()

    override fun save(grant: VenueAccessGrant): VenueAccessGrant {
        storage[grant.id] = grant
        return grant
    }

    override fun findById(id: UUID): VenueAccessGrant? = storage[id]

    override fun findByVenueId(venueId: UUID): List<VenueAccessGrant> =
        storage.values.filter { it.venueId == venueId }

    override fun findByVenueIdAndStatus(venueId: UUID, status: VenueAccessGrantStatus): List<VenueAccessGrant> =
        storage.values.filter { it.venueId == venueId && it.status == status }

    override fun findApprovedByVenueIdAndOrgId(venueId: UUID, orgId: UUID): VenueAccessGrant? =
        storage.values.firstOrNull {
            it.venueId == venueId &&
            it.requestingOrgId == orgId &&
            it.status == VenueAccessGrantStatus.APPROVED
        }
}

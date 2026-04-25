package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.Venue
import java.util.UUID

interface VenueRepository {
    fun save(venue: Venue): Venue
    fun findById(id: UUID): Venue?
    fun findBySpaceId(spaceId: UUID): Venue?
    fun findAll(): List<Venue>
    fun findByOrganizationId(organizationId: UUID): List<Venue>
    fun deleteById(id: UUID): Boolean
}

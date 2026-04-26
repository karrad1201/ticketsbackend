package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.VenueApplication
import com.karrad.bilets.domain.enums.VenueApplicationStatus
import java.util.UUID

interface VenueApplicationRepository {
    fun save(application: VenueApplication): VenueApplication
    fun findById(id: UUID): VenueApplication?
    fun findByOrganizationId(organizationId: UUID): List<VenueApplication>
    fun findByStatus(status: VenueApplicationStatus): List<VenueApplication>
    fun findAll(): List<VenueApplication>
    fun deleteById(id: UUID): Boolean
}

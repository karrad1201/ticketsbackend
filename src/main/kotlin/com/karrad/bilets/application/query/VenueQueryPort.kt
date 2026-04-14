package com.karrad.bilets.application.query

import com.karrad.bilets.domain.entity.Venue
import java.util.UUID

interface VenueQueryPort {
    fun findAll(): List<Venue>
    fun findById(id: UUID): Venue?
}

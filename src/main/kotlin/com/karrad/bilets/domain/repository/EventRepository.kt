package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.Event
import java.util.UUID

interface EventRepository {
    fun save(event: Event): Event
    fun findById(id: UUID): Event?
    fun findAll(): List<Event>
    fun findByVenueId(venueId: UUID): List<Event>
    fun deleteById(id: UUID): Boolean
}

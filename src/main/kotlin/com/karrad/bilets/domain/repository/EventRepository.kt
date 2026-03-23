package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.Event
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class EventSearchCriteria(
    val query: String?,
    val city: String?,
    val categoryId: UUID?,
    val venueId: UUID?,
    val dateFrom: LocalDate?,
    val dateTo: LocalDate?,
    val now: Instant
)

interface EventRepository {
    fun save(event: Event): Event
    fun findById(id: UUID): Event?
    fun findAll(): List<Event>
    fun findByVenueId(venueId: UUID): List<Event>
    fun findAvailableByCity(city: String, now: Instant): List<Event>
    fun searchAvailable(criteria: EventSearchCriteria): List<Event>
    fun findIdsWithStartedOpenSales(now: Instant, limit: Int): List<UUID>
    fun deleteById(id: UUID): Boolean
}

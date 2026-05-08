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
    fun findAllByIds(ids: Collection<UUID>): List<Event> = ids.mapNotNull { findById(it) }
    fun findAll(): List<Event>
    fun findAll(offset: Int, limit: Int): List<Event> = findAll().drop(offset).take(limit)
    fun findByVenueId(venueId: UUID): List<Event>
    fun findAvailableByCity(city: String, now: Instant): List<Event>
    /** Fetch available events for city with optional date filter and row cap applied at SQL level. */
    fun findAvailableByCity(city: String, now: Instant, date: LocalDate?, limit: Int): List<Event> =
        findAvailableByCity(city, now).let { events ->
            val filtered = if (date != null) {
                events.filter { it.time.atOffset(java.time.ZoneOffset.UTC).toLocalDate() == date }
            } else events
            filtered.take(limit)
        }
    fun searchAvailable(criteria: EventSearchCriteria): List<Event>
    fun findUpcomingByOrganizationId(organizationId: UUID, now: Instant): List<Event>
    fun findIdsWithStartedOpenSales(now: Instant, limit: Int): List<UUID>
    fun findByGroupId(groupId: UUID): List<Event> = findAll().filter { it.groupId == groupId }
    fun deleteById(id: UUID): Boolean
}

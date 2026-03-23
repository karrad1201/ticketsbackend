package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.entity.LayoutTemplate
import com.karrad.bilets.domain.entity.Row
import com.karrad.bilets.domain.entity.Section
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.TicketType
import com.karrad.bilets.domain.entity.UserEventVisit
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.VenueSpace
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InMemoryRepositoriesTests {

    @Test
    fun `venue repository should save update list and delete venues`() {
        val repository = InMemoryVenueRepository()
        val venue = demoVenue()

        val saved = repository.save(venue)
        val updated = repository.save(saved.copy(label = "Updated Hall"))

        assertEquals("Updated Hall", repository.findById(venue.id)?.label)
        assertEquals(listOf(updated), repository.findAll())
        assertTrue(repository.deleteById(venue.id))
        assertNull(repository.findById(venue.id))
        assertFalse(repository.deleteById(venue.id))
    }

    @Test
    fun `category repository should save update list and delete categories`() {
        val repository = InMemoryCategoryRepository()
        val category = Category(code = "theatre", label = "Theatre")

        val saved = repository.save(category)
        val updated = repository.save(saved.copy(label = "Drama"))

        assertEquals("Drama", repository.findById(category.id)?.label)
        assertEquals(updated, repository.findByCode("theatre"))
        assertEquals(listOf(updated), repository.findAll())
        assertTrue(repository.deleteById(category.id))
        assertNull(repository.findById(category.id))
        assertFalse(repository.deleteById(category.id))
    }

    @Test
    fun `venue repository should find venue by space id`() {
        val repository = InMemoryVenueRepository()
        val venue = demoVenue()
        repository.save(venue)

        val result = repository.findBySpaceId(venue.spaces.first().id)

        assertEquals(venue, result)
    }

    @Test
    fun `layout template repository should find templates by venue space id`() {
        val repository = InMemoryLayoutTemplateRepository()
        val mainHallId = UUID.fromString("123e4567-e89b-12d3-a456-426614174100")
        val smallHallId = UUID.fromString("123e4567-e89b-12d3-a456-426614174101")
        val first = repository.save(demoLayoutTemplate(mainHallId))
        repository.save(demoLayoutTemplate(smallHallId))

        val result = repository.findByVenueSpaceId(mainHallId)

        assertEquals(listOf(first), result)
    }

    @Test
    fun `layout template repository should delete by id`() {
        val repository = InMemoryLayoutTemplateRepository()
        val template = repository.save(demoLayoutTemplate(UUID.fromString("123e4567-e89b-12d3-a456-426614174102")))

        assertTrue(repository.deleteById(template.id))
        assertNull(repository.findById(template.id))
        assertFalse(repository.deleteById(template.id))
    }

    @Test
    fun `event repository should find events by venue id`() {
        val venueRepository = InMemoryVenueRepository()
        val repository = InMemoryEventRepository(venueRepository)
        val venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614174110")
        val otherVenueId = UUID.fromString("123e4567-e89b-12d3-a456-426614174111")
        venueRepository.save(demoVenue(id = venueId))
        venueRepository.save(demoVenue(id = otherVenueId))
        val first = repository.save(demoEvent(venueId = venueId))
        repository.save(demoEvent(id = UUID.fromString("123e4567-e89b-12d3-a456-426614174112"), venueId = otherVenueId))

        val result = repository.findByVenueId(venueId)

        assertEquals(listOf(first), result)
    }

    @Test
    fun `event repository should save update list and delete events`() {
        val venueRepository = InMemoryVenueRepository()
        venueRepository.save(demoVenue())
        val repository = InMemoryEventRepository(venueRepository)
        val event = demoEvent()
        val saved = repository.save(event)
        val updated = repository.save(saved.copy(label = "Updated Event"))

        assertEquals("Updated Event", repository.findById(event.id)?.label)
        assertEquals(listOf(updated), repository.findAll())
        assertTrue(repository.deleteById(event.id))
        assertNull(repository.findById(event.id))
        assertFalse(repository.deleteById(event.id))
    }

    @Test
    fun `event repository should search available events by city and filters`() {
        val venueRepository = InMemoryVenueRepository()
        val firstVenue = demoVenue()
        val secondVenue = demoVenue(id = UUID.fromString("123e4567-e89b-12d3-a456-426614174113"))
        venueRepository.save(firstVenue)
        venueRepository.save(secondVenue.copy(city = City(label = "Perm", subject = Subject(label = "Perm Krai"))))
        val repository = InMemoryEventRepository(venueRepository)
        val available = repository.save(
            demoEvent(venueId = firstVenue.id).copy(
                label = "Arena Night",
                time = Instant.parse("2026-04-02T18:00:00Z")
            )
        )
        repository.save(
            demoEvent(
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614174114"),
                venueId = firstVenue.id
            )
                .copy(label = "Closed Night")
                .closeSales(Instant.parse("2026-04-01T10:00:00Z"))
        )
        repository.save(
            demoEvent(
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614174115"),
                venueId = secondVenue.id
            ).copy(
                label = "Perm Arena",
                time = Instant.parse("2026-04-02T18:00:00Z")
            )
        )

        val byCity = repository.findAvailableByCity("Ekaterinburg", Instant.parse("2026-04-01T12:00:00Z"))
        val bySearch = repository.searchAvailable(
            com.karrad.bilets.domain.repository.EventSearchCriteria(
                query = "arena",
                city = "Ekaterinburg",
                categoryId = null,
                venueId = firstVenue.id,
                dateFrom = java.time.LocalDate.parse("2026-04-02"),
                dateTo = java.time.LocalDate.parse("2026-04-02"),
                now = Instant.parse("2026-04-01T12:00:00Z")
            )
        )

        assertEquals(listOf(available), byCity)
        assertEquals(listOf(available), bySearch)
    }

    @Test
    fun `event inventory plan repository should save update list and delete by event id`() {
        val repository = InMemoryEventInventoryPlanRepository()
        val event = demoEvent(venueSpaceId = null)
        val saved = repository.save(
            EventInventoryPlan.generalAdmission(
                event = event,
                ticketTypes = listOf(TicketType(label = "Standard", price = 1500, quota = 100))
            )
        )
        val updated = repository.save(
            saved.copy(admissionInventory = saved.admissionInventory.map { it.copy(capacity = 120) })
        )

        assertEquals(120, repository.findByEventId(event.id)?.admissionInventory?.first()?.capacity)
        assertEquals(listOf(updated), repository.findAll())
        assertTrue(repository.deleteByEventId(event.id))
        assertNull(repository.findByEventId(event.id))
        assertFalse(repository.deleteByEventId(event.id))
    }

    @Test
    fun `user event visit repository should save update list and delete visits`() {
        val repository = InMemoryUserEventVisitRepository()
        val visit = UserEventVisit(
            userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174130"),
            eventId = UUID.fromString("123e4567-e89b-12d3-a456-426614174131"),
            visitedAt = Instant.parse("2026-04-01T18:00:00Z")
        )
        val saved = repository.save(visit)
        val updated = repository.save(saved.copy(visitedAt = Instant.parse("2026-04-02T18:00:00Z")))

        assertEquals(updated, repository.findById(visit.id))
        assertEquals(listOf(updated), repository.findByUserId(visit.userId))
        assertEquals(listOf(updated), repository.findAll())
        assertTrue(repository.deleteById(visit.id))
        assertNull(repository.findById(visit.id))
        assertFalse(repository.deleteById(visit.id))
    }

    private fun demoVenue(id: UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174001")): Venue {
        return Venue(
            label = "Demo Hall",
            city = City(label = "Ekaterinburg", subject = Subject(label = "Sverdlovsk Oblast")),
            id = id,
            spaces = listOf(VenueSpace(label = "Main Hall"))
        )
    }

    private fun demoLayoutTemplate(venueSpaceId: UUID): LayoutTemplate {
        return LayoutTemplate(
            venueSpaceId = venueSpaceId,
            label = "Theatre Layout",
            sections = listOf(
                Section(
                    label = "Партер",
                    key = "parter",
                    rows = listOf(
                        Row(label = "Ряд 1", key = "r1", startSeat = 1, endSeat = 3, price = 2000)
                    )
                )
            )
        )
    }

    private fun demoEvent(
        id: UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174120"),
        venueId: UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174121"),
        venueSpaceId: UUID? = UUID.fromString("123e4567-e89b-12d3-a456-426614174122")
    ): Event {
        return Event(
            label = "Demo Event",
            description = "Repository test event",
            venueId = venueId,
            categoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174123"),
            time = Instant.parse("2026-04-01T18:00:00Z"),
            venueSpaceId = venueSpaceId,
            id = id
        )
    }
}

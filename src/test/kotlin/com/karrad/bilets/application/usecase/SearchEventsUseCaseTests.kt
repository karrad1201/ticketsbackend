package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.VenueSpace
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.VenueRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@SpringJUnitConfig(ApplicationServicesTestConfig::class)
@Import(SearchEventsUseCase::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SearchEventsUseCaseTests {

    @Autowired
    lateinit var venueRepository: VenueRepository

    @Autowired
    lateinit var eventRepository: EventRepository

    @Autowired
    lateinit var useCase: SearchEventsUseCase

    @Test
    fun `should search by text and filters`() {
        venueRepository.save(demoVenue())
        eventRepository.save(demoEvent(label = "Rock Arena", venueId = venueId(), categoryId = categoryId(), time = Instant.parse("2026-05-01T18:00:00Z")))
        eventRepository.save(demoEvent(id = UUID.fromString("123e4567-e89b-12d3-a456-426614176303"), label = "Arena Afterparty", venueId = venueId(), categoryId = categoryId(), time = Instant.parse("2026-05-02T18:00:00Z")))
        eventRepository.save(demoEvent(id = UUID.fromString("123e4567-e89b-12d3-a456-426614176304"), label = "Jazz Night", venueId = venueId(), categoryId = categoryId(), time = Instant.parse("2026-05-03T18:00:00Z")))

        val result = useCase.search(
            query = "arena",
            city = "Ekaterinburg",
            categoryId = categoryId(),
            venueId = venueId(),
            dateFrom = LocalDate.parse("2026-05-01"),
            dateTo = LocalDate.parse("2026-05-02"),
            page = 0,
            size = 10
        )

        assertEquals(listOf("Arena Afterparty", "Rock Arena"), result.map { it.label })
    }

    @Test
    fun `should reject invalid search date range`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.search(
                query = null,
                city = null,
                categoryId = null,
                venueId = null,
                dateFrom = LocalDate.parse("2026-05-03"),
                dateTo = LocalDate.parse("2026-05-01"),
                page = 0,
                size = 10
            )
        }

        assertEquals("dateFrom must be before or equal to dateTo", exception.message)
    }

    private fun demoVenue(): Venue =
        Venue(
            label = "Demo Hall",
            city = City(label = "Ekaterinburg", subject = Subject(label = "Sverdlovsk Oblast")),
            organizationId = UUID.fromString("123e4567-e89b-12d3-a456-426614176301"),
            id = venueId(),
            spaces = listOf(VenueSpace(label = "Main Hall"))
        )

    private fun demoEvent(id: UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614176302"), label: String, venueId: UUID, categoryId: UUID, time: Instant): Event =
        Event(
            label = label,
            description = "Search test event",
            venueId = venueId,
            categoryId = categoryId,
            time = time,
            venueSpaceId = null,
            id = id,
            organizationId = UUID.fromString("123e4567-e89b-12d3-a456-426614176301")
        )

    private fun venueId(): UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614176300")
    private fun categoryId(): UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614176305")
}

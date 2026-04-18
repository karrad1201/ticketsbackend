package com.karrad.bilets.web

import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.VenueSpace
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.VenueRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.Instant
import java.util.UUID

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EventSearchIntegrationTests {

    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    lateinit var venueRepository: VenueRepository

    @Autowired
    lateinit var eventRepository: EventRepository

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Test
    fun `should search events by text and filters over http`() {
        venueRepository.save(demoVenue())
        eventRepository.save(demoEvent(UUID.fromString("123e4567-e89b-12d3-a456-426614176502"), "Rock Arena", Instant.parse("2026-05-01T18:00:00Z")))
        eventRepository.save(demoEvent(UUID.fromString("123e4567-e89b-12d3-a456-426614176503"), "Arena Afterparty", Instant.parse("2026-05-02T18:00:00Z")))

        mockMvc.perform(
            get("/api/v1/events/search")
                .param("q", "arena")
                .param("city", "Ekaterinburg")
                .param("venueId", venueId().toString())
                .param("categoryId", categoryId().toString())
                .param("dateFrom", "2026-05-01")
                .param("dateTo", "2026-05-02")
                .param("page", "0")
                .param("size", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].label").value("Arena Afterparty"))
            .andExpect(jsonPath("$[1].label").value("Rock Arena"))
    }

    @Test
    fun `should reject invalid search page size over http`() {
        mockMvc.perform(
            get("/api/v1/events/search")
                .param("size", "51")
        )
            .andExpect(status().isBadRequest)
    }

    private fun demoVenue(): Venue = Venue(
        label = "Demo Hall",
        city = City(label = "Ekaterinburg", subject = Subject(label = "Sverdlovsk Oblast")),
        organizationId = UUID.fromString("123e4567-e89b-12d3-a456-426614176500"),
        id = venueId(),
        spaces = listOf(VenueSpace(label = "Main Hall"))
    )

    private fun demoEvent(id: UUID, label: String, time: Instant): Event = Event(
        label = label,
        description = "Search controller event",
        venueId = venueId(),
        categoryId = categoryId(),
        time = time,
        venueSpaceId = null,
        id = id,
        organizationId = UUID.fromString("123e4567-e89b-12d3-a456-426614176500")
    )

    private fun venueId(): UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614176501")
    private fun categoryId(): UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614176504")
}

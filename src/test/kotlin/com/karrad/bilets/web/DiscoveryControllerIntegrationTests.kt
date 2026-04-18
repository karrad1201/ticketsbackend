package com.karrad.bilets.web

import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.entity.UserEventVisit
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.VenueSpace
import com.karrad.bilets.domain.repository.CategoryRepository
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.domain.repository.UserEventVisitRepository
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
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DiscoveryControllerIntegrationTests {

    lateinit var mockMvc: MockMvc

    @Autowired lateinit var authTokenRepository: AuthTokenRepository
    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    lateinit var categoryRepository: CategoryRepository

    @Autowired
    lateinit var venueRepository: VenueRepository

    @Autowired
    lateinit var eventRepository: EventRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var userEventVisitRepository: UserEventVisitRepository

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Test
    fun `should return sectioned discovery response`() {
        categoryRepository.save(Category(code = "rock", label = "Rock", id = categoryId()))
        venueRepository.save(demoVenue())
        userRepository.save(User(email = "viewer@example.com", fullName = "Viewer", id = userId()))
        eventRepository.save(demoEvent(UUID.fromString("123e4567-e89b-12d3-a456-426614176410"), "Past Visit", LocalDate.now(ZoneOffset.UTC).minusDays(1).atTime(12, 0).toInstant(ZoneOffset.UTC)))
        eventRepository.save(demoEvent(UUID.fromString("123e4567-e89b-12d3-a456-426614176411"), "Tomorrow Show", LocalDate.now(ZoneOffset.UTC).plusDays(1).atTime(18, 0).toInstant(ZoneOffset.UTC)))
        userEventVisitRepository.save(UserEventVisit(userId = userId(), eventId = UUID.fromString("123e4567-e89b-12d3-a456-426614176410"), visitedAt = Instant.now()))

        mockMvc.perform(
            get("/api/v1/discovery")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(userId())}")
                .param("city", "Ekaterinburg")
                .param("page", "0")
                .param("size", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.forYou[0].label").value("Tomorrow Show"))
            .andExpect(jsonPath("$.byCategory[0].events[0].label").value("Tomorrow Show"))
            .andExpect(jsonPath("$.tomorrow[0].label").value("Tomorrow Show"))
    }

    @Test
    fun `should reject oversized discovery page size`() {
        mockMvc.perform(
            get("/api/v1/discovery")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(userId())}")
                .param("city", "Ekaterinburg")
                .param("size", "51")
        )
            .andExpect(status().isBadRequest)
    }

    private fun demoVenue(): Venue = Venue(
        label = "Demo Hall",
        city = City(label = "Ekaterinburg", subject = Subject(label = "Sverdlovsk Oblast")),
        organizationId = UUID.fromString("123e4567-e89b-12d3-a456-426614176400"),
        id = UUID.fromString("123e4567-e89b-12d3-a456-426614176401"),
        spaces = listOf(VenueSpace(label = "Main Hall"))
    )

    private fun demoEvent(id: UUID, label: String, time: Instant): Event = Event(
        label = label,
        description = "Discovery controller event",
        venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614176401"),
        categoryId = categoryId(),
        time = time,
        venueSpaceId = null,
        id = id,
        organizationId = UUID.fromString("123e4567-e89b-12d3-a456-426614176400")
    )

    private fun userId(): UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614176402")
    private fun categoryId(): UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614176403")
}

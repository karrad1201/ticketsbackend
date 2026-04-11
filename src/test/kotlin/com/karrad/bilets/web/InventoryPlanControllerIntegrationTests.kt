package com.karrad.bilets.web

import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.TicketType
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.repository.CategoryRepository
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.UserRepository
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
class InventoryPlanControllerIntegrationTests {

    lateinit var mockMvc: MockMvc

    @Autowired lateinit var webApplicationContext: WebApplicationContext
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var eventRepository: EventRepository
    @Autowired lateinit var eventInventoryPlanRepository: EventInventoryPlanRepository
    @Autowired lateinit var venueRepository: VenueRepository
    @Autowired lateinit var organizationRepository: OrganizationRepository
    @Autowired lateinit var categoryRepository: CategoryRepository
    @Autowired lateinit var authTokenRepository: AuthTokenRepository

    private val userId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")
    private val orgId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001")
    private val venueId = UUID.fromString("cccccccc-0000-0000-0000-000000000001")
    private val categoryId = UUID.fromString("dddddddd-0000-0000-0000-000000000001")
    private val eventId = UUID.fromString("eeeeeeee-0000-0000-0000-000000000001")

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Test
    fun `should return empty list when no inventory plans`() {
        mockMvc.perform(
            get("/api/inventory-plans")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(userId)}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `should return 404 when inventory plan not found for event`() {
        val unknownEventId = UUID.fromString("ffffffff-0000-0000-0000-000000000001")

        mockMvc.perform(
            get("/api/inventory-plans/$unknownEventId")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(userId)}")
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `should return inventory plan by event id`() {
        seedEvent()
        val event = eventRepository.findById(eventId)!!
        val plan = EventInventoryPlan.generalAdmission(
            event = event,
            ticketTypes = listOf(TicketType(label = "Standard", price = 1500, quota = 100))
        )
        eventInventoryPlanRepository.save(plan)

        mockMvc.perform(
            get("/api/inventory-plans/$eventId")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(userId)}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.eventId").value(eventId.toString()))
    }

    @Test
    fun `should list all inventory plans`() {
        seedEvent()
        val event = eventRepository.findById(eventId)!!
        val plan = EventInventoryPlan.generalAdmission(
            event = event,
            ticketTypes = listOf(TicketType(label = "Standard", price = 1500, quota = 50))
        )
        eventInventoryPlanRepository.save(plan)

        mockMvc.perform(
            get("/api/inventory-plans")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(userId)}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    private fun seedEvent() {
        organizationRepository.save(Organization(code = "test-org", name = "Test Org", id = orgId))
        venueRepository.save(
            Venue(
                label = "Hall",
                city = City(label = "Moscow", subject = Subject(label = "Moscow Oblast")),
                organizationId = orgId,
                id = venueId
            )
        )
        categoryRepository.save(Category(code = "music", label = "Music", id = categoryId))
        eventRepository.save(
            Event(
                label = "Concert",
                description = "Live music",
                venueId = venueId,
                categoryId = categoryId,
                time = Instant.parse("2027-01-01T18:00:00Z"),
                organizationId = orgId,
                id = eventId
            )
        )
    }
}

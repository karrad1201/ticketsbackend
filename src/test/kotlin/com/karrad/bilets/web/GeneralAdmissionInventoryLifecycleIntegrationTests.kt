package com.karrad.bilets.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.entity.TicketType
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.Instant
import java.util.UUID

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GeneralAdmissionInventoryLifecycleIntegrationTests {

    lateinit var mockMvc: MockMvc
    lateinit var userBearer: String

    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var eventRepository: EventRepository

    @Autowired
    lateinit var eventInventoryPlanRepository: EventInventoryPlanRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var authTokenRepository: AuthTokenRepository

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        val user = User(
            fullName = "Test User",
            email = "user@example.com",
            role = UserRole.USER,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614175000")
        )
        userRepository.save(user)
        userBearer = "Bearer ${authTokenRepository.bearerFor(user.id)}"
    }

    @Test
    fun `should hold release and sell general admission over http`() {
        val event = generalAdmissionEvent()
        eventRepository.save(event)
        eventInventoryPlanRepository.save(generalAdmissionPlan(event))

        val payload = objectMapper.writeValueAsString(
            mapOf(
                "items" to listOf(
                    mapOf("ticketTypeId" to standardTicketTypeId(), "quantity" to 5)
                )
            )
        )

        mockMvc.perform(
            post("/api/v1/events/${event.id}/inventory/general-admission/holds")
                .header("Authorization", userBearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.admissionInventory[0].held").value(5))

        val releasePayload = objectMapper.writeValueAsString(
            mapOf(
                "items" to listOf(
                    mapOf("ticketTypeId" to standardTicketTypeId(), "quantity" to 2)
                )
            )
        )

        mockMvc.perform(
            post("/api/v1/events/${event.id}/inventory/general-admission/releases")
                .header("Authorization", userBearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(releasePayload)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.admissionInventory[0].held").value(3))

        val salePayload = objectMapper.writeValueAsString(
            mapOf(
                "items" to listOf(
                    mapOf("ticketTypeId" to standardTicketTypeId(), "quantity" to 3)
                )
            )
        )

        mockMvc.perform(
            post("/api/v1/events/${event.id}/inventory/general-admission/sales")
                .header("Authorization", userBearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(salePayload)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.admissionInventory[0].held").value(0))
            .andExpect(jsonPath("$.admissionInventory[0].sold").value(3))

        mockMvc.perform(get("/api/v1/events/${event.id}/inventory"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.eventId").value(event.id.toString()))
    }

    private fun generalAdmissionEvent(): Event {
        return Event(
            label = "Festival",
            description = "Open floor event",
            venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614175001"),
            categoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614175004"),
            time = Instant.parse("2026-04-01T18:00:00Z"),
            venueSpaceId = null,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614175002")
        )
    }

    private fun generalAdmissionPlan(event: Event): EventInventoryPlan {
        return EventInventoryPlan.generalAdmission(
            event = event,
            ticketTypes = listOf(
                TicketType(label = "Standard", price = 1500, quota = 100, id = standardTicketTypeId())
            )
        )
    }

    private fun standardTicketTypeId(): UUID =
        UUID.fromString("123e4567-e89b-12d3-a456-426614175003")
}

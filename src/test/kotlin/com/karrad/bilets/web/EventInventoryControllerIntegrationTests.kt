package com.karrad.bilets.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.LayoutTemplate
import com.karrad.bilets.domain.entity.Row
import com.karrad.bilets.domain.entity.Section
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.LayoutTemplateRepository
import com.karrad.bilets.domain.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.UUID

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EventInventoryControllerIntegrationTests {

    lateinit var mockMvc: MockMvc
    lateinit var adminBearer: String

    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var eventRepository: EventRepository

    @Autowired
    lateinit var layoutTemplateRepository: LayoutTemplateRepository

    @Autowired
    lateinit var eventInventoryPlanRepository: EventInventoryPlanRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var authTokenRepository: AuthTokenRepository

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        val admin = User(
            fullName = "Admin",
            email = "admin@example.com",
            role = UserRole.ADMIN,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174300")
        )
        userRepository.save(admin)
        adminBearer = "Bearer ${authTokenRepository.bearerFor(admin.id)}"
    }

    @Test
    fun `should create seated inventory plan over http`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        layoutTemplateRepository.save(layoutTemplate)

        mockMvc.perform(
            post("/api/events/${event.id}/inventory/seated")
                .header("Authorization", adminBearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf("layoutTemplateId" to layoutTemplate.id)
                    )
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.mode").value("SEATED"))
            .andExpect(jsonPath("$.seatInventory.length()").value(3))
            .andExpect(jsonPath("$.eventId").value(event.id.toString()))
    }

    @Test
    fun `should create general admission inventory plan over http`() {
        val event = generalAdmissionEvent()
        eventRepository.save(event)

        mockMvc.perform(
            post("/api/events/${event.id}/inventory/general-admission")
                .header("Authorization", adminBearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "ticketTypes" to listOf(
                                mapOf("label" to "Standard", "price" to 1500, "quota" to 100),
                                mapOf("label" to "VIP", "price" to 3000, "quota" to 20)
                            )
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.mode").value("GENERAL_ADMISSION"))
            .andExpect(jsonPath("$.admissionInventory.length()").value(2))
            .andExpect(jsonPath("$.eventId").value(event.id.toString()))
    }

    @Test
    fun `should fail when seated inventory is requested for missing event`() {
        val layoutTemplate = seatedLayoutTemplate(UUID.fromString("123e4567-e89b-12d3-a456-426614174301"))
        layoutTemplateRepository.save(layoutTemplate)

        mockMvc.perform(
            post("/api/events/123e4567-e89b-12d3-a456-426614174302/inventory/seated")
                .header("Authorization", adminBearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf("layoutTemplateId" to layoutTemplate.id)
                    )
                )
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should fail when inventory already exists`() {
        val event = generalAdmissionEvent()
        eventRepository.save(event)

        val payload = objectMapper.writeValueAsString(
            mapOf(
                "ticketTypes" to listOf(
                    mapOf("label" to "Standard", "price" to 1500, "quota" to 100)
                )
            )
        )

        mockMvc.perform(
            post("/api/events/${event.id}/inventory/general-admission")
                .header("Authorization", adminBearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        )
            .andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/events/${event.id}/inventory/general-admission")
                .header("Authorization", adminBearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        )
            .andExpect(status().isConflict)
    }

    private fun seatedEvent(): Event {
        return Event(
            label = "Hamlet",
            description = "Evening show",
            venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614174310"),
            categoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174316"),
            time = Instant.parse("2026-04-01T18:00:00Z"),
            venueSpaceId = UUID.fromString("123e4567-e89b-12d3-a456-426614174311"),
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174312")
        )
    }

    private fun generalAdmissionEvent(): Event {
        return Event(
            label = "Festival",
            description = "Open floor event",
            venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614174313"),
            categoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174317"),
            time = Instant.parse("2026-04-01T18:00:00Z"),
            venueSpaceId = null,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174314")
        )
    }

    private fun seatedLayoutTemplate(venueSpaceId: UUID): LayoutTemplate {
        return LayoutTemplate(
            venueSpaceId = venueSpaceId,
            label = "Main Hall Layout",
            sections = listOf(
                Section(
                    label = "Партер",
                    key = "parter",
                    rows = listOf(
                        Row(label = "Ряд 1", key = "r1", startSeat = 1, endSeat = 3, price = 2000)
                    )
                )
            ),
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174315")
        )
    }
}

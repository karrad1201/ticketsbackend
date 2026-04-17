package com.karrad.bilets.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.karrad.bilets.domain.entity.AdmissionQuantity
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.entity.LayoutTemplate
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.Row
import com.karrad.bilets.domain.entity.SeatKey
import com.karrad.bilets.domain.entity.Section
import com.karrad.bilets.domain.entity.TicketType
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.SeatStatus
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
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
class OrderControllerIntegrationTests {

    lateinit var mockMvc: MockMvc
    lateinit var adminBearer: String

    @Autowired lateinit var authTokenRepository: AuthTokenRepository
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
    lateinit var organizationRepository: OrganizationRepository

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        val admin = User(
            email = "admin@example.com",
            fullName = "Platform Admin",
            role = UserRole.ADMIN,
            id = java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614177099")
        )
        userRepository.save(admin)
        adminBearer = "Bearer ${authTokenRepository.bearerFor(admin.id)}"
    }

    @Test
    fun `should create confirm order and return issued tickets`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        organizationRepository.save(organization())
        userRepository.save(buyer())
        eventInventoryPlanRepository.save(EventInventoryPlan.seated(event, layoutTemplate))

        val createResponse = mockMvc.perform(
            post("/api/events/${event.id}/orders")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(buyerUserId())}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "seatKeys" to listOf(
                                mapOf("sectionKey" to "parter", "rowKey" to "r1", "seatKey" to "1")
                            )
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
            .andExpect(jsonPath("$.amount").value(2000))
            .andReturn()

        val orderId = objectMapper.readTree(createResponse.response.contentAsString).get("id").asText()

        mockMvc.perform(
            post("/api/orders/$orderId/confirm-payment")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(buyerUserId())}")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PAID"))

        mockMvc.perform(
            get("/api/orders/$orderId/tickets")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(buyerUserId())}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].userId").value(buyerUserId().toString()))
            .andExpect(jsonPath("$[0].seatKey.sectionKey").value("parter"))

        val plan = requireNotNull(eventInventoryPlanRepository.findByEventId(event.id))
        val seat = plan.seatInventory.first { it.seatKey.seatKey == "1" }
        org.junit.jupiter.api.Assertions.assertEquals(SeatStatus.SOLD, seat.status)
        org.junit.jupiter.api.Assertions.assertEquals(
            1800,
            requireNotNull(organizationRepository.findById(organizationId())).balance
        )
    }

    @Test
    fun `should create and confirm general admission order over http`() {
        val event = generalAdmissionEvent()
        eventRepository.save(event)
        organizationRepository.save(organization())
        userRepository.save(buyer())
        eventInventoryPlanRepository.save(generalAdmissionPlan(event))

        val createResponse = mockMvc.perform(
            post("/api/events/${event.id}/orders")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(buyerUserId())}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "admissionItems" to listOf(
                                mapOf("ticketTypeId" to standardTicketTypeId(), "quantity" to 2)
                            )
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
            .andExpect(jsonPath("$.amount").value(3000))
            .andReturn()

        val orderId = objectMapper.readTree(createResponse.response.contentAsString).get("id").asText()

        mockMvc.perform(
            post("/api/orders/$orderId/confirm-payment")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(buyerUserId())}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PAID"))

        mockMvc.perform(get("/api/tickets/me").header("Authorization", "Bearer ${authTokenRepository.bearerFor(buyerUserId())}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].ticketTypeId").value(standardTicketTypeId().toString()))

        org.junit.jupiter.api.Assertions.assertEquals(
            2700,
            requireNotNull(organizationRepository.findById(organizationId())).balance
        )
    }

    @Test
    fun `should expire order over http and release held seat`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        organizationRepository.save(organization())
        userRepository.save(buyer())
        eventInventoryPlanRepository.save(EventInventoryPlan.seated(event, layoutTemplate))

        val createResponse = mockMvc.perform(
            post("/api/events/${event.id}/orders")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(buyerUserId())}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "seatKeys" to listOf(
                                mapOf("sectionKey" to "parter", "rowKey" to "r1", "seatKey" to "2")
                            )
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
            .andReturn()

        val orderId = objectMapper.readTree(createResponse.response.contentAsString).get("id").asText()

        mockMvc.perform(
            post("/api/orders/$orderId/expire")
                .header("Authorization", adminBearer)
        )
            .andExpect(status().isConflict)

        val plan = requireNotNull(eventInventoryPlanRepository.findByEventId(event.id))
        org.junit.jupiter.api.Assertions.assertEquals(
            SeatStatus.HELD,
            plan.seatInventory.first { it.seatKey.seatKey == "2" }.status
        )
    }

    @Test
    fun `should reject order creation over http when request has no items`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        organizationRepository.save(organization())
        userRepository.save(buyer())
        eventInventoryPlanRepository.save(EventInventoryPlan.seated(event, layoutTemplate))

        mockMvc.perform(
            post("/api/events/${event.id}/orders")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(buyerUserId())}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyMap<String, Any>()))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should reject order creation over http when both seats and admission items are provided`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        organizationRepository.save(organization())
        userRepository.save(buyer())
        eventInventoryPlanRepository.save(EventInventoryPlan.seated(event, layoutTemplate))

        mockMvc.perform(
            post("/api/events/${event.id}/orders")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(buyerUserId())}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "seatKeys" to listOf(
                                mapOf("sectionKey" to "parter", "rowKey" to "r1", "seatKey" to "1")
                            ),
                            "admissionItems" to listOf(
                                mapOf("ticketTypeId" to standardTicketTypeId(), "quantity" to 1)
                            )
                        )
                    )
                )
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should reject order creation over http when seat is unavailable`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        organizationRepository.save(organization())
        userRepository.save(buyer())
        val heldPlan = EventInventoryPlan.seated(event, layoutTemplate).holdSeats(
            listOf(SeatKey(sectionKey = "parter", rowKey = "r1", seatKey = "1"))
        )
        eventInventoryPlanRepository.save(heldPlan)

        mockMvc.perform(
            post("/api/events/${event.id}/orders")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(buyerUserId())}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "seatKeys" to listOf(
                                mapOf("sectionKey" to "parter", "rowKey" to "r1", "seatKey" to "1")
                            )
                        )
                    )
                )
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should reject repeated payment confirmation over http`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        organizationRepository.save(organization())
        userRepository.save(buyer())
        eventInventoryPlanRepository.save(EventInventoryPlan.seated(event, layoutTemplate))

        val createResponse = mockMvc.perform(
            post("/api/events/${event.id}/orders")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(buyerUserId())}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "seatKeys" to listOf(
                                mapOf("sectionKey" to "parter", "rowKey" to "r1", "seatKey" to "1")
                            )
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
            .andReturn()

        val orderId = objectMapper.readTree(createResponse.response.contentAsString).get("id").asText()

        mockMvc.perform(
            post("/api/orders/$orderId/confirm-payment")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(buyerUserId())}")
        )
            .andExpect(status().isOk)

        mockMvc.perform(
            post("/api/orders/$orderId/confirm-payment")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(buyerUserId())}")
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `should return existing order by id`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        organizationRepository.save(organization())
        userRepository.save(buyer())
        eventInventoryPlanRepository.save(EventInventoryPlan.seated(event, layoutTemplate))

        val createResponse = mockMvc.perform(
            post("/api/events/${event.id}/orders")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(buyerUserId())}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "seatKeys" to listOf(
                                mapOf("sectionKey" to "parter", "rowKey" to "r1", "seatKey" to "1")
                            )
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
            .andReturn()

        val orderId = objectMapper.readTree(createResponse.response.contentAsString).get("id").asText()

        mockMvc.perform(
            get("/api/orders/$orderId")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(buyerUserId())}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(orderId))
            .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
            .andExpect(jsonPath("$.buyerUserId").value(buyerUserId().toString()))
    }

    @Test
    fun `should return not found for unknown order`() {
        mockMvc.perform(get("/api/orders/123e4567-e89b-12d3-a456-426614177999"))
            .andExpect(status().isNotFound)
    }

    private fun seatedEvent(): Event {
        return Event(
            label = "Hamlet",
            description = "Evening show",
            venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614177001"),
            categoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614177002"),
            time = Instant.parse("2026-06-01T18:00:00Z"),
            venueSpaceId = UUID.fromString("123e4567-e89b-12d3-a456-426614177003"),
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614177004"),
            organizationId = organizationId()
        )
    }

    private fun generalAdmissionEvent(): Event {
        return Event(
            label = "Festival",
            description = "Open floor event",
            venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614177011"),
            categoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614177012"),
            time = Instant.parse("2026-06-01T20:00:00Z"),
            venueSpaceId = null,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614177013"),
            organizationId = organizationId()
        )
    }

    private fun seatedLayoutTemplate(venueSpaceId: UUID): LayoutTemplate {
        return LayoutTemplate(
            venueSpaceId = venueSpaceId,
            label = "Main Hall Layout",
            sections = listOf(
                Section(
                    label = "Parter",
                    key = "parter",
                    rows = listOf(
                        Row(label = "Row 1", key = "r1", startSeat = 1, endSeat = 2, price = 2000)
                    )
                )
            ),
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614177005")
        )
    }

    private fun buyerUserId(): UUID =
        UUID.fromString("123e4567-e89b-12d3-a456-426614177006")

    private fun organizationId(): UUID =
        UUID.fromString("123e4567-e89b-12d3-a456-426614177015")

    private fun standardTicketTypeId(): UUID =
        UUID.fromString("123e4567-e89b-12d3-a456-426614177014")

    private fun generalAdmissionPlan(event: Event): EventInventoryPlan {
        return EventInventoryPlan.generalAdmission(
            event = event,
            ticketTypes = listOf(
                TicketType(label = "Standard", price = 1500, quota = 100, id = standardTicketTypeId())
            )
        )
    }

    private fun buyer(): User =
        User(
            email = "order-buyer@example.com",
            fullName = "Order Buyer",
            id = buyerUserId()
        )

    private fun organization(): Organization =
        Organization(
            code = "order-org",
            name = "Order Org",
            id = organizationId()
        )
}

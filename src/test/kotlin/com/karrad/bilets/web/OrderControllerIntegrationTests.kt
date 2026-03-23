package com.karrad.bilets.web

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.entity.LayoutTemplate
import com.karrad.bilets.domain.entity.Row
import com.karrad.bilets.domain.entity.Section
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.SeatStatus
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
class OrderControllerIntegrationTests {

    lateinit var mockMvc: MockMvc

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

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Test
    fun `should create confirm order and return issued tickets`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        userRepository.save(buyer())
        eventInventoryPlanRepository.save(EventInventoryPlan.seated(event, layoutTemplate))

        val createResponse = mockMvc.perform(
            post("/api/events/${event.id}/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "buyerUserId" to buyerUserId(),
                            "seatKeys" to listOf(
                                mapOf("sectionKey" to "parter", "rowKey" to "r1", "seatNumber" to 1)
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
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PAID"))

        mockMvc.perform(get("/api/orders/$orderId/tickets"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].userId").value(buyerUserId().toString()))
            .andExpect(jsonPath("$[0].seatKey.sectionKey").value("parter"))

        val plan = requireNotNull(eventInventoryPlanRepository.findByEventId(event.id))
        val seat = plan.seatInventory.first { it.seatKey.seatNumber == 1 }
        org.junit.jupiter.api.Assertions.assertEquals(SeatStatus.SOLD, seat.status)
    }

    private fun seatedEvent(): Event {
        return Event(
            label = "Hamlet",
            description = "Evening show",
            venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614177001"),
            categoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614177002"),
            time = Instant.parse("2026-04-01T18:00:00Z"),
            venueSpaceId = UUID.fromString("123e4567-e89b-12d3-a456-426614177003"),
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614177004")
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

    private fun buyer(): User =
        User(
            email = "order-buyer@example.com",
            fullName = "Order Buyer",
            id = buyerUserId()
        )
}

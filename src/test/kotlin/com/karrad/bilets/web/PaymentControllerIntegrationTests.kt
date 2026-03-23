package com.karrad.bilets.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.TicketType
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.PaymentCallbackStatus
import com.karrad.bilets.domain.enums.PaymentAttemptStatus
import com.karrad.bilets.domain.enums.SeatStatus
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.PaymentAttemptRepository
import com.karrad.bilets.domain.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
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
class PaymentControllerIntegrationTests {

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
    lateinit var organizationRepository: OrganizationRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var paymentAttemptRepository: PaymentAttemptRepository

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Test
    fun `should confirm payment through mock callback and issue tickets`() {
        val event = generalAdmissionEvent()
        eventRepository.save(event)
        organizationRepository.save(organization())
        userRepository.save(buyer())
        eventInventoryPlanRepository.save(generalAdmissionPlan(event))

        val createResponse = mockMvc.perform(
            post("/api/events/${event.id}/orders")
                .header("X-User-Id", buyerId().toString())
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
            .andReturn()

        val orderJson = objectMapper.readTree(createResponse.response.contentAsString)
        val orderId = UUID.fromString(orderJson.get("id").asText())
        val paymentReference = paymentAttemptRepository.findByOrderId(orderId)?.reference
            ?: error("Payment attempt not created for order $orderId")

        mockMvc.perform(
            post("/api/payments/callbacks/mock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "paymentReference" to paymentReference,
                            "status" to "SUCCEEDED",
                            "payload" to "{\"provider\":\"mock\"}"
                        )
                    )
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PAID"))

        mockMvc.perform(get("/api/tickets/me").header("X-User-Id", buyerId().toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))

        assertEquals(
            PaymentAttemptStatus.SUCCEEDED,
            paymentAttemptRepository.findByOrderId(orderId)?.status
        )
        assertEquals(
            2700,
            organizationRepository.findById(organizationId())?.balance
        )
    }

    @Test
    fun `should reject ticket listing without current user header`() {
        userRepository.save(buyer())

        mockMvc.perform(get("/api/tickets/me"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.detail").value("Missing X-User-Id header"))
    }

    @Test
    fun `should release held inventory when mock callback returns expired`() {
        val event = generalAdmissionEvent()
        eventRepository.save(event)
        organizationRepository.save(organization())
        userRepository.save(buyer())
        eventInventoryPlanRepository.save(generalAdmissionPlan(event))

        val createResponse = mockMvc.perform(
            post("/api/events/${event.id}/orders")
                .header("X-User-Id", buyerId().toString())
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
            .andReturn()

        val orderJson = objectMapper.readTree(createResponse.response.contentAsString)
        val orderId = UUID.fromString(orderJson.get("id").asText())
        val paymentReference = paymentAttemptRepository.findByOrderId(orderId)?.reference
            ?: error("Payment attempt not created for order $orderId")

        mockMvc.perform(
            post("/api/payments/callbacks/mock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "paymentReference" to paymentReference,
                            "status" to PaymentCallbackStatus.EXPIRED.name
                        )
                    )
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PAYMENT_FAILED"))

        assertEquals(
            PaymentAttemptStatus.FAILED,
            paymentAttemptRepository.findByOrderId(orderId)?.status
        )
        val inventory = eventInventoryPlanRepository.findByEventId(event.id)
            ?.admissionInventory
            ?.first { it.ticketTypeId == standardTicketTypeId() }
            ?: error("Inventory not found")
        assertEquals(0, inventory.held)
        assertEquals(0, inventory.sold)
    }

    private fun organizationId(): UUID =
        UUID.fromString("223e4567-e89b-12d3-a456-426614174001")

    private fun buyerId(): UUID =
        UUID.fromString("223e4567-e89b-12d3-a456-426614174002")

    private fun eventId(): UUID =
        UUID.fromString("223e4567-e89b-12d3-a456-426614174003")

    private fun standardTicketTypeId(): UUID =
        UUID.fromString("223e4567-e89b-12d3-a456-426614174004")

    private fun organization() = Organization(
        code = "ural-tours",
        name = "Ural Tours",
        id = organizationId()
    )

    private fun buyer() = User(
        email = "daria@example.com",
        fullName = "Daria Petrova",
        id = buyerId()
    )

    private fun generalAdmissionEvent() = Event(
        label = "Night Museum",
        description = "Late evening guided program",
        venueId = UUID.fromString("223e4567-e89b-12d3-a456-426614174006"),
        categoryId = UUID.fromString("223e4567-e89b-12d3-a456-426614174005"),
        time = Instant.parse("2032-07-01T19:00:00Z"),
        venueSpaceId = null,
        id = eventId(),
        organizationId = organizationId()
    )

    private fun generalAdmissionPlan(event: Event) = EventInventoryPlan.generalAdmission(
        event = event,
        ticketTypes = listOf(
            TicketType(
                label = "Standard",
                id = standardTicketTypeId(),
                price = 1500,
                quota = 5
            )
        )
    )
}

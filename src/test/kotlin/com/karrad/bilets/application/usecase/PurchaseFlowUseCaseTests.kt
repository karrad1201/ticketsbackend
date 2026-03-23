package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.AdmissionQuantity
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.entity.LayoutTemplate
import com.karrad.bilets.domain.entity.Row
import com.karrad.bilets.domain.entity.SeatKey
import com.karrad.bilets.domain.entity.Section
import com.karrad.bilets.domain.entity.TicketType
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.OrderStatus
import com.karrad.bilets.domain.enums.SeatStatus
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrderRepository
import com.karrad.bilets.domain.repository.TicketRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.infrastructure.payment.MockPaymentGateway
import com.karrad.bilets.support.MutableClock
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@SpringJUnitConfig(ApplicationServicesTestConfig::class)
@Import(CreateOrderUseCase::class, ConfirmOrderPaymentUseCase::class, ExpireOrderUseCase::class)
@TestPropertySource(properties = ["purchase.hold-ttl=PT30M"])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PurchaseFlowUseCaseTests {

    @Autowired
    lateinit var eventRepository: EventRepository

    @Autowired
    lateinit var eventInventoryPlanRepository: EventInventoryPlanRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var ticketRepository: TicketRepository

    @Autowired
    lateinit var paymentGateway: MockPaymentGateway

    @Autowired
    lateinit var clock: MutableClock

    @Autowired
    lateinit var createOrderUseCase: CreateOrderUseCase

    @Autowired
    lateinit var confirmOrderPaymentUseCase: ConfirmOrderPaymentUseCase

    @Autowired
    lateinit var expireOrderUseCase: ExpireOrderUseCase

    @Test
    fun `should create seated order hold seats and start payment`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        userRepository.save(buyer())
        eventInventoryPlanRepository.save(EventInventoryPlan.seated(event, layoutTemplate))

        val order = createOrderUseCase.create(
            CreateOrderCommand(
                eventId = event.id,
                buyerUserId = buyerUserId(),
                seatKeys = listOf(
                    SeatKey(sectionKey = "parter", rowKey = "r1", seatNumber = 1),
                    SeatKey(sectionKey = "parter", rowKey = "r1", seatNumber = 2)
                )
            )
        )

        assertEquals(OrderStatus.PENDING_PAYMENT, order.status)
        assertEquals(4000, order.amount)
        assertEquals(clock.instant().plus(30, ChronoUnit.MINUTES), order.expiresAt)
        assertTrue(order.paymentReference.isNotBlank())
        assertEquals(1, paymentGateway.createdPayments().size)

        val plan = requireNotNull(eventInventoryPlanRepository.findByEventId(event.id))
        assertEquals(SeatStatus.HELD, plan.seatInventory.first { it.seatNumber == 1 }.status)
        assertEquals(SeatStatus.HELD, plan.seatInventory.first { it.seatNumber == 2 }.status)
    }

    @Test
    fun `should confirm payment sell inventory and issue tickets`() {
        val event = generalAdmissionEvent()
        eventRepository.save(event)
        userRepository.save(buyer())
        eventInventoryPlanRepository.save(generalAdmissionPlan(event))

        val order = createOrderUseCase.create(
            CreateOrderCommand(
                eventId = event.id,
                buyerUserId = buyerUserId(),
                admissionItems = listOf(
                    AdmissionQuantity(ticketTypeId = standardTicketTypeId(), quantity = 2)
                )
            )
        )

        val paidOrder = confirmOrderPaymentUseCase.confirm(order.id)

        assertEquals(OrderStatus.PAID, paidOrder.status)

        val inventory = requireNotNull(eventInventoryPlanRepository.findByEventId(event.id))
            .admissionInventory
            .first { it.ticketTypeId == standardTicketTypeId() }
        assertEquals(0, inventory.held)
        assertEquals(2, inventory.sold)

        val issuedTickets = ticketRepository.findByOrderId(order.id)
        assertEquals(2, issuedTickets.size)
        assertTrue(issuedTickets.all { it.userId == buyerUserId() })
        assertTrue(issuedTickets.all { it.ticketTypeId == standardTicketTypeId() })
    }

    @Test
    fun `should expire order and release hold after ttl`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        userRepository.save(buyer())
        eventInventoryPlanRepository.save(EventInventoryPlan.seated(event, layoutTemplate))

        val order = createOrderUseCase.create(
            CreateOrderCommand(
                eventId = event.id,
                buyerUserId = buyerUserId(),
                seatKeys = listOf(SeatKey(sectionKey = "parter", rowKey = "r1", seatNumber = 1))
            )
        )

        clock.advanceByMinutes(31)
        val expired = expireOrderUseCase.expire(order.id)

        assertEquals(OrderStatus.EXPIRED, expired.status)
        val plan = requireNotNull(eventInventoryPlanRepository.findByEventId(event.id))
        assertEquals(SeatStatus.AVAILABLE, plan.seatInventory.first { it.seatNumber == 1 }.status)
        assertTrue(ticketRepository.findByOrderId(order.id).isEmpty())
    }

    @Test
    fun `should allow only first concurrent purchase request for same seat`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        userRepository.save(buyer())
        eventInventoryPlanRepository.save(EventInventoryPlan.seated(event, layoutTemplate))

        val executor = Executors.newFixedThreadPool(2)
        try {
            val task = Callable {
                runCatching {
                            createOrderUseCase.create(
                                CreateOrderCommand(
                                    eventId = event.id,
                                    buyerUserId = buyerUserId(),
                                    seatKeys = listOf(SeatKey(sectionKey = "parter", rowKey = "r1", seatNumber = 1))
                                )
                            )
                }
            }

            val futures: List<Future<Result<com.karrad.bilets.domain.entity.Order>>> =
                listOf(executor.submit(task), executor.submit(task))
            val results = futures.map { it.get() }

            assertEquals(1, results.count { it.isSuccess })
            assertEquals(1, results.count { it.isFailure })
            assertEquals(1, paymentGateway.createdPayments().size)
            assertEquals(1, orderRepository.findAll().size)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `should reject payment confirmation after ttl elapsed`() {
        val event = generalAdmissionEvent()
        eventRepository.save(event)
        userRepository.save(buyer())
        eventInventoryPlanRepository.save(generalAdmissionPlan(event))

        val order = createOrderUseCase.create(
            CreateOrderCommand(
                eventId = event.id,
                buyerUserId = buyerUserId(),
                admissionItems = listOf(AdmissionQuantity(ticketTypeId = standardTicketTypeId(), quantity = 1))
            )
        )

        clock.advanceByMinutes(31)

        val exception = assertFailsWith<IllegalStateException> {
            confirmOrderPaymentUseCase.confirm(order.id)
        }

        assertTrue(exception.message!!.contains("expired"))
        assertEquals(OrderStatus.EXPIRED, requireNotNull(orderRepository.findById(order.id)).status)
    }

    private fun seatedEvent(): Event {
        return Event(
            label = "Hamlet",
            description = "Evening show",
            venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614176001"),
            categoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614176002"),
            time = Instant.parse("2026-04-01T18:00:00Z"),
            venueSpaceId = UUID.fromString("123e4567-e89b-12d3-a456-426614176003"),
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614176004")
        )
    }

    private fun generalAdmissionEvent(): Event {
        return Event(
            label = "Festival",
            description = "Open floor event",
            venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614176005"),
            categoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614176006"),
            time = Instant.parse("2026-04-01T18:00:00Z"),
            venueSpaceId = null,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614176007")
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
                        Row(label = "Row 1", key = "r1", startSeat = 1, endSeat = 3, price = 2000)
                    )
                )
            ),
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614176008")
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
        UUID.fromString("123e4567-e89b-12d3-a456-426614176009")

    private fun buyerUserId(): UUID =
        UUID.fromString("123e4567-e89b-12d3-a456-426614176010")

    private fun buyer(): User =
        User(
            email = "buyer@example.com",
            fullName = "Demo Buyer",
            id = buyerUserId()
        )
}

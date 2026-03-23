package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.application.service.PaymentReconciliationService
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
import com.karrad.bilets.domain.enums.OrderStatus
import com.karrad.bilets.domain.enums.PaymentAttemptStatus
import com.karrad.bilets.domain.enums.PaymentCallbackStatus
import com.karrad.bilets.domain.enums.SeatStatus
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrderRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.PaymentAttemptRepository
import com.karrad.bilets.domain.repository.PaymentCallbackAuditRepository
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
@Import(
    CreateOrderUseCase::class,
    ConfirmOrderPaymentUseCase::class,
    ExpireOrderUseCase::class,
    HandlePaymentCallbackUseCase::class,
    CloseEventSalesUseCase::class,
    ProcessStalePaymentAttemptsUseCase::class
)
@TestPropertySource(properties = ["purchase.hold-ttl=PT30M", "purchase.platform-commission-rate=0.10"])
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
    lateinit var organizationRepository: OrganizationRepository

    @Autowired
    lateinit var ticketRepository: TicketRepository

    @Autowired
    lateinit var paymentAttemptRepository: PaymentAttemptRepository

    @Autowired
    lateinit var paymentCallbackAuditRepository: PaymentCallbackAuditRepository

    @Autowired
    lateinit var paymentGateway: MockPaymentGateway

    @Autowired
    lateinit var clock: MutableClock

    @Autowired
    lateinit var paymentReconciliationService: PaymentReconciliationService

    @Autowired
    lateinit var createOrderUseCase: CreateOrderUseCase

    @Autowired
    lateinit var confirmOrderPaymentUseCase: ConfirmOrderPaymentUseCase

    @Autowired
    lateinit var expireOrderUseCase: ExpireOrderUseCase

    @Autowired
    lateinit var handlePaymentCallbackUseCase: HandlePaymentCallbackUseCase

    @Autowired
    lateinit var closeEventSalesUseCase: CloseEventSalesUseCase

    @Autowired
    lateinit var processStalePaymentAttemptsUseCase: ProcessStalePaymentAttemptsUseCase

    @Test
    fun `should create seated order hold seats and start payment`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        organizationRepository.save(organization())
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
        organizationRepository.save(organization())
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
        assertEquals(2700, requireNotNull(organizationRepository.findById(organizationId())).balance)
    }

    @Test
    fun `should expire order and release hold after ttl`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        organizationRepository.save(organization())
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
    fun `should expire general admission order and release held inventory after ttl`() {
        val event = generalAdmissionEvent()
        eventRepository.save(event)
        organizationRepository.save(organization())
        userRepository.save(buyer())
        eventInventoryPlanRepository.save(generalAdmissionPlan(event))

        val order = createOrderUseCase.create(
            CreateOrderCommand(
                eventId = event.id,
                buyerUserId = buyerUserId(),
                admissionItems = listOf(AdmissionQuantity(ticketTypeId = standardTicketTypeId(), quantity = 2))
            )
        )

        clock.advanceByMinutes(31)
        val expired = expireOrderUseCase.expire(order.id)

        assertEquals(OrderStatus.EXPIRED, expired.status)
        val inventory = requireNotNull(eventInventoryPlanRepository.findByEventId(event.id))
            .admissionInventory
            .first { it.ticketTypeId == standardTicketTypeId() }
        assertEquals(0, inventory.held)
        assertEquals(0, inventory.sold)
    }

    @Test
    fun `should allow only first concurrent purchase request for same seat`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        organizationRepository.save(organization())
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
        organizationRepository.save(organization())
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
        assertEquals(PaymentAttemptStatus.FAILED, requireNotNull(paymentAttemptRepository.findByOrderId(order.id)).status)
        val inventory = requireNotNull(eventInventoryPlanRepository.findByEventId(event.id))
            .admissionInventory
            .first { it.ticketTypeId == standardTicketTypeId() }
        assertEquals(0, inventory.held)
    }

    @Test
    fun `should confirm payment through callback idempotently`() {
        val event = generalAdmissionEvent()
        eventRepository.save(event)
        organizationRepository.save(organization())
        userRepository.save(buyer())
        eventInventoryPlanRepository.save(generalAdmissionPlan(event))

        val order = createOrderUseCase.create(
            CreateOrderCommand(
                eventId = event.id,
                buyerUserId = buyerUserId(),
                admissionItems = listOf(AdmissionQuantity(ticketTypeId = standardTicketTypeId(), quantity = 1))
            )
        )

        val first = handlePaymentCallbackUseCase.handle(
            HandlePaymentCallbackCommand(
                paymentReference = order.paymentReference,
                status = PaymentCallbackStatus.SUCCEEDED,
                receivedAt = clock.instant()
            )
        )
        val second = handlePaymentCallbackUseCase.handle(
            HandlePaymentCallbackCommand(
                paymentReference = order.paymentReference,
                status = PaymentCallbackStatus.SUCCEEDED,
                receivedAt = clock.instant().plusSeconds(5)
            )
        )

        assertEquals(OrderStatus.PAID, first.status)
        assertEquals(OrderStatus.PAID, second.status)
        assertEquals(1, ticketRepository.findByOrderId(order.id).size)
        assertEquals(PaymentAttemptStatus.SUCCEEDED, requireNotNull(paymentAttemptRepository.findByOrderId(order.id)).status)
        assertEquals(2, paymentCallbackAuditRepository.findByPaymentReference(order.paymentReference).size)
    }

    @Test
    fun `should fail payment through callback and release held inventory`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        organizationRepository.save(organization())
        userRepository.save(buyer())
        eventInventoryPlanRepository.save(EventInventoryPlan.seated(event, layoutTemplate))

        val order = createOrderUseCase.create(
            CreateOrderCommand(
                eventId = event.id,
                buyerUserId = buyerUserId(),
                seatKeys = listOf(SeatKey(sectionKey = "parter", rowKey = "r1", seatNumber = 1))
            )
        )

        val failedOrder = handlePaymentCallbackUseCase.handle(
            HandlePaymentCallbackCommand(
                paymentReference = order.paymentReference,
                status = PaymentCallbackStatus.FAILED,
                receivedAt = clock.instant(),
                failureReason = "Card declined"
            )
        )

        assertEquals(OrderStatus.PAYMENT_FAILED, failedOrder.status)
        assertEquals(PaymentAttemptStatus.FAILED, requireNotNull(paymentAttemptRepository.findByOrderId(order.id)).status)
        assertEquals(
            SeatStatus.AVAILABLE,
            requireNotNull(eventInventoryPlanRepository.findByEventId(event.id))
                .seatInventory.first { it.seatNumber == 1 }.status
        )
    }

    @Test
    fun `should expire payment through callback and release held inventory`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        organizationRepository.save(organization())
        userRepository.save(buyer())
        eventInventoryPlanRepository.save(EventInventoryPlan.seated(event, layoutTemplate))

        val order = createOrderUseCase.create(
            CreateOrderCommand(
                eventId = event.id,
                buyerUserId = buyerUserId(),
                seatKeys = listOf(SeatKey(sectionKey = "parter", rowKey = "r1", seatNumber = 1))
            )
        )

        val expiredOrder = handlePaymentCallbackUseCase.handle(
            HandlePaymentCallbackCommand(
                paymentReference = order.paymentReference,
                status = PaymentCallbackStatus.EXPIRED,
                receivedAt = clock.instant()
            )
        )

        assertEquals(OrderStatus.PAYMENT_FAILED, expiredOrder.status)
        assertEquals(PaymentAttemptStatus.FAILED, requireNotNull(paymentAttemptRepository.findByOrderId(order.id)).status)
        assertEquals(
            SeatStatus.AVAILABLE,
            requireNotNull(eventInventoryPlanRepository.findByEventId(event.id))
                .seatInventory.first { it.seatNumber == 1 }.status
        )
    }

    @Test
    fun `should reject order creation for started event`() {
        val event = generalAdmissionEvent().copy(time = clock.instant())
        eventRepository.save(event)
        organizationRepository.save(organization())
        userRepository.save(buyer())
        eventInventoryPlanRepository.save(generalAdmissionPlan(event))

        val exception = assertFailsWith<IllegalArgumentException> {
            createOrderUseCase.create(
                CreateOrderCommand(
                    eventId = event.id,
                    buyerUserId = buyerUserId(),
                    admissionItems = listOf(AdmissionQuantity(ticketTypeId = standardTicketTypeId(), quantity = 1))
                )
            )
        }

        assertTrue(exception.message!!.contains("Ticket sales are closed"))
    }

    @Test
    fun `should reject order creation for manually closed event`() {
        val event = generalAdmissionEvent().closeSales(clock.instant())
        eventRepository.save(event)
        organizationRepository.save(organization())
        userRepository.save(buyer())
        eventInventoryPlanRepository.save(generalAdmissionPlan(event))

        val exception = assertFailsWith<IllegalArgumentException> {
            createOrderUseCase.create(
                CreateOrderCommand(
                    eventId = event.id,
                    buyerUserId = buyerUserId(),
                    admissionItems = listOf(AdmissionQuantity(ticketTypeId = standardTicketTypeId(), quantity = 1))
                )
            )
        }

        assertTrue(exception.message!!.contains("Ticket sales are closed"))
    }

    @Test
    fun `should close event sales and fail pending orders`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        organizationRepository.save(organization())
        userRepository.save(buyer())
        eventInventoryPlanRepository.save(EventInventoryPlan.seated(event, layoutTemplate))

        val order = createOrderUseCase.create(
            CreateOrderCommand(
                eventId = event.id,
                buyerUserId = buyerUserId(),
                seatKeys = listOf(SeatKey(sectionKey = "parter", rowKey = "r1", seatNumber = 1))
            )
        )

        clock.advanceByMinutes(14 * 24 * 60)
        val closedEvent = closeEventSalesUseCase.closeWhenStarted(event.id)

        assertEquals(event.id, closedEvent.id)
        assertEquals(OrderStatus.PAYMENT_FAILED, requireNotNull(orderRepository.findById(order.id)).status)
        assertEquals(PaymentAttemptStatus.FAILED, requireNotNull(paymentAttemptRepository.findByOrderId(order.id)).status)
        assertEquals(
            SeatStatus.AVAILABLE,
            requireNotNull(eventInventoryPlanRepository.findByEventId(event.id))
                .seatInventory.first { it.seatNumber == 1 }.status
        )
    }

    @Test
    fun `should process stale payment attempts through expire flow`() {
        val event = generalAdmissionEvent()
        eventRepository.save(event)
        organizationRepository.save(organization())
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

        val processed = processStalePaymentAttemptsUseCase.process()

        assertEquals(listOf(order.id), processed.map { it.id })
        assertEquals(OrderStatus.EXPIRED, requireNotNull(orderRepository.findById(order.id)).status)
        assertEquals(PaymentAttemptStatus.FAILED, requireNotNull(paymentAttemptRepository.findByOrderId(order.id)).status)
    }

    @Test
    fun `should list stale pending payment attempts for reconciliation`() {
        val event = generalAdmissionEvent()
        eventRepository.save(event)
        organizationRepository.save(organization())
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

        val staleAttempts = paymentReconciliationService.findStalePendingAttempts(clock.instant())

        assertEquals(listOf(order.id), staleAttempts.map { it.orderId })
    }

    @Test
    fun `should reject order creation when neither seats nor admission items are provided`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        organizationRepository.save(organization())
        userRepository.save(buyer())
        eventInventoryPlanRepository.save(EventInventoryPlan.seated(event, layoutTemplate))

        val exception = assertFailsWith<IllegalArgumentException> {
            createOrderUseCase.create(
                CreateOrderCommand(
                    eventId = event.id,
                    buyerUserId = buyerUserId()
                )
            )
        }

        assertTrue(exception.message!!.contains("must contain seats or admission items"))
    }

    @Test
    fun `should reject order creation when both seats and admission items are provided`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        organizationRepository.save(organization())
        userRepository.save(buyer())
        eventInventoryPlanRepository.save(EventInventoryPlan.seated(event, layoutTemplate))

        val exception = assertFailsWith<IllegalArgumentException> {
            createOrderUseCase.create(
                CreateOrderCommand(
                    eventId = event.id,
                    buyerUserId = buyerUserId(),
                    seatKeys = listOf(SeatKey(sectionKey = "parter", rowKey = "r1", seatNumber = 1)),
                    admissionItems = listOf(AdmissionQuantity(ticketTypeId = standardTicketTypeId(), quantity = 1))
                )
            )
        }

        assertTrue(exception.message!!.contains("either seatKeys or admissionItems"))
    }

    @Test
    fun `should reject order creation when buyer does not exist`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        organizationRepository.save(organization())
        eventInventoryPlanRepository.save(EventInventoryPlan.seated(event, layoutTemplate))

        val exception = assertFailsWith<IllegalArgumentException> {
            createOrderUseCase.create(
                CreateOrderCommand(
                    eventId = event.id,
                    buyerUserId = UUID.fromString("123e4567-e89b-12d3-a456-426614176099"),
                    seatKeys = listOf(SeatKey(sectionKey = "parter", rowKey = "r1", seatNumber = 1))
                )
            )
        }

        assertTrue(exception.message!!.contains("User not found"))
    }

    @Test
    fun `should reject order creation when event inventory plan does not exist`() {
        val event = seatedEvent()
        eventRepository.save(event)
        organizationRepository.save(organization())
        userRepository.save(buyer())

        val exception = assertFailsWith<IllegalArgumentException> {
            createOrderUseCase.create(
                CreateOrderCommand(
                    eventId = event.id,
                    buyerUserId = buyerUserId(),
                    seatKeys = listOf(SeatKey(sectionKey = "parter", rowKey = "r1", seatNumber = 1))
                )
            )
        }

        assertTrue(exception.message!!.contains("EventInventoryPlan not found"))
    }

    @Test
    fun `should reject confirming already paid order`() {
        val event = generalAdmissionEvent()
        eventRepository.save(event)
        organizationRepository.save(organization())
        userRepository.save(buyer())
        eventInventoryPlanRepository.save(generalAdmissionPlan(event))

        val order = createOrderUseCase.create(
            CreateOrderCommand(
                eventId = event.id,
                buyerUserId = buyerUserId(),
                admissionItems = listOf(AdmissionQuantity(ticketTypeId = standardTicketTypeId(), quantity = 1))
            )
        )

        confirmOrderPaymentUseCase.confirm(order.id)

        val exception = assertFailsWith<IllegalStateException> {
            confirmOrderPaymentUseCase.confirm(order.id)
        }

        assertTrue(exception.message!!.contains("already paid"))
    }

    @Test
    fun `should reject expiring order before ttl`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        organizationRepository.save(organization())
        userRepository.save(buyer())
        eventInventoryPlanRepository.save(EventInventoryPlan.seated(event, layoutTemplate))

        val order = createOrderUseCase.create(
            CreateOrderCommand(
                eventId = event.id,
                buyerUserId = buyerUserId(),
                seatKeys = listOf(SeatKey(sectionKey = "parter", rowKey = "r1", seatNumber = 1))
            )
        )

        val exception = assertFailsWith<IllegalStateException> {
            expireOrderUseCase.expire(order.id)
        }

        assertTrue(exception.message!!.contains("not expired yet"))
    }

    @Test
    fun `should reject expiring already paid order`() {
        val event = generalAdmissionEvent()
        eventRepository.save(event)
        organizationRepository.save(organization())
        userRepository.save(buyer())
        eventInventoryPlanRepository.save(generalAdmissionPlan(event))

        val order = createOrderUseCase.create(
            CreateOrderCommand(
                eventId = event.id,
                buyerUserId = buyerUserId(),
                admissionItems = listOf(AdmissionQuantity(ticketTypeId = standardTicketTypeId(), quantity = 1))
            )
        )
        confirmOrderPaymentUseCase.confirm(order.id)
        clock.advanceByMinutes(31)

        val exception = assertFailsWith<IllegalStateException> {
            expireOrderUseCase.expire(order.id)
        }

        assertTrue(exception.message!!.contains("Only pending order can expire"))
    }

    @Test
    fun `should return already expired order without changing inventory`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        organizationRepository.save(organization())
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
        val repeated = expireOrderUseCase.expire(order.id)

        assertEquals(OrderStatus.EXPIRED, repeated.status)
        assertEquals(expired.id, repeated.id)
        val plan = requireNotNull(eventInventoryPlanRepository.findByEventId(event.id))
        assertEquals(SeatStatus.AVAILABLE, plan.seatInventory.first { it.seatNumber == 1 }.status)
    }

    private fun seatedEvent(): Event {
        return Event(
            label = "Hamlet",
            description = "Evening show",
            venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614176001"),
            categoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614176002"),
            time = Instant.parse("2026-04-01T18:00:00Z"),
            venueSpaceId = UUID.fromString("123e4567-e89b-12d3-a456-426614176003"),
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614176004"),
            organizationId = organizationId()
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
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614176007"),
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

    private fun organizationId(): UUID =
        UUID.fromString("123e4567-e89b-12d3-a456-426614176011")

    private fun organization(): Organization =
        Organization(
            code = "demo-org",
            name = "Demo Org",
            id = organizationId()
        )

    private fun buyer(): User =
        User(
            email = "buyer@example.com",
            fullName = "Demo Buyer",
            id = buyerUserId()
        )
}

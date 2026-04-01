package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.AdmissionQuantity
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.SeatKey
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.OrderStatus
import com.karrad.bilets.domain.enums.SeatStatus
import com.karrad.bilets.domain.repository.OrderRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.TicketRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.infrastructure.payment.MockPaymentGateway
import com.karrad.bilets.support.MutableClock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@SpringJUnitConfig(JdbcDurableOrderFlowTestConfig::class)
@Import(CreateOrderUseCase::class, ConfirmOrderPaymentUseCase::class, ExpireOrderUseCase::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class JdbcDurableOrderFlowTests {

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var eventRepository: EventRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var organizationRepository: OrganizationRepository

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

    @BeforeEach
    fun seedCommonData() {
        organizationRepository.save(organization())
        userRepository.save(buyer())
        eventRepository.save(seatedEvent())
        eventRepository.save(generalAdmissionEvent())
    }

    @Test
    fun `should create seated order and persist held seat in database`() {
        insertSeat(seatedEvent().id, seat(1), 2000)
        insertSeat(seatedEvent().id, seat(2), 2500)

        val order = createOrderUseCase.create(
            CreateOrderCommand(
                eventId = seatedEvent().id,
                buyerUserId = buyer().id,
                seatKeys = listOf(seat(1), seat(2))
            )
        )

        assertEquals(OrderStatus.PENDING_PAYMENT, order.status)
        assertEquals(4500, order.amount)
        assertEquals(1, paymentGateway.createdPayments().size)
        assertEquals(SeatStatus.HELD.name, seatStatus(seatedEvent().id, seat(1)))
        assertEquals(SeatStatus.HELD.name, seatStatus(seatedEvent().id, seat(2)))
        assertEquals(order.id, heldOrderId(seatedEvent().id, seat(1)))
        assertEquals(1, orderRepository.findAll().size)
    }

    @Test
    fun `should confirm general admission order and persist sold inventory tickets and org balance`() {
        insertAdmissionInventory(generalAdmissionEvent().id, standardTicketTypeId(), price = 1500, capacity = 100)

        val order = createOrderUseCase.create(
            CreateOrderCommand(
                eventId = generalAdmissionEvent().id,
                buyerUserId = buyer().id,
                admissionItems = listOf(AdmissionQuantity(ticketTypeId = standardTicketTypeId(), quantity = 2))
            )
        )

        val paidOrder = confirmOrderPaymentUseCase.confirm(order.id)

        assertEquals(OrderStatus.PAID, paidOrder.status)
        assertEquals(0, admissionHeld(generalAdmissionEvent().id, standardTicketTypeId()))
        assertEquals(2, admissionSold(generalAdmissionEvent().id, standardTicketTypeId()))
        assertEquals(2, ticketRepository.findByOrderId(order.id).size)
        assertEquals(2700, requireNotNull(organizationRepository.findById(organization().id)).balance)
    }

    @Test
    fun `should expire seated order and release held seat in database`() {
        insertSeat(seatedEvent().id, seat(1), 2000)

        val order = createOrderUseCase.create(
            CreateOrderCommand(
                eventId = seatedEvent().id,
                buyerUserId = buyer().id,
                seatKeys = listOf(seat(1))
            )
        )

        clock.advanceByMinutes(31)
        val expired = expireOrderUseCase.expire(order.id)

        assertEquals(OrderStatus.EXPIRED, expired.status)
        assertEquals(SeatStatus.AVAILABLE.name, seatStatus(seatedEvent().id, seat(1)))
        assertEquals(null, heldOrderId(seatedEvent().id, seat(1)))
    }

    @Test
    fun `should reject order creation for manually closed event in jdbc mode`() {
        insertAdmissionInventory(generalAdmissionEvent().id, standardTicketTypeId(), price = 1500, capacity = 100)
        eventRepository.save(generalAdmissionEvent().closeSales(clock.instant()))

        val exception = assertFailsWith<IllegalArgumentException> {
            createOrderUseCase.create(
                CreateOrderCommand(
                    eventId = generalAdmissionEvent().id,
                    buyerUserId = buyer().id,
                    admissionItems = listOf(AdmissionQuantity(ticketTypeId = standardTicketTypeId(), quantity = 1))
                )
            )
        }

        assertTrue(exception.message!!.contains("Ticket sales are closed"))
    }

    @Test
    fun `should reject second purchase for already held seat`() {
        insertSeat(seatedEvent().id, seat(1), 2000)

        createOrderUseCase.create(
            CreateOrderCommand(
                eventId = seatedEvent().id,
                buyerUserId = buyer().id,
                seatKeys = listOf(seat(1))
            )
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            createOrderUseCase.create(
                CreateOrderCommand(
                    eventId = seatedEvent().id,
                    buyerUserId = buyer().id,
                    seatKeys = listOf(seat(1))
                )
            )
        }

        assertTrue(exception.message!!.contains("Seats are not available"))
    }

    @Test
    fun `should rollback seat holds when one seat in request is unavailable`() {
        insertSeat(seatedEvent().id, seat(1), 2000)
        insertSeat(seatedEvent().id, seat(2), 2500)

        createOrderUseCase.create(
            CreateOrderCommand(
                eventId = seatedEvent().id,
                buyerUserId = buyer().id,
                seatKeys = listOf(seat(2))
            )
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            createOrderUseCase.create(
                CreateOrderCommand(
                    eventId = seatedEvent().id,
                    buyerUserId = buyer().id,
                    seatKeys = listOf(seat(1), seat(2))
                )
            )
        }

        assertTrue(exception.message!!.contains("Seats are not available"))
        assertEquals(SeatStatus.AVAILABLE.name, seatStatus(seatedEvent().id, seat(1)))
    }

    @Test
    fun `should rollback admission holds when one ticket type cannot be reserved`() {
        val vipTicketTypeId = UUID.fromString("123e4567-e89b-12d3-a456-426614179099")
        insertAdmissionInventory(generalAdmissionEvent().id, standardTicketTypeId(), price = 1500, capacity = 10)
        insertAdmissionInventory(generalAdmissionEvent().id, vipTicketTypeId, price = 3000, capacity = 1)

        createOrderUseCase.create(
            CreateOrderCommand(
                eventId = generalAdmissionEvent().id,
                buyerUserId = buyer().id,
                admissionItems = listOf(AdmissionQuantity(ticketTypeId = vipTicketTypeId, quantity = 1))
            )
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            createOrderUseCase.create(
                CreateOrderCommand(
                    eventId = generalAdmissionEvent().id,
                    buyerUserId = buyer().id,
                    admissionItems = listOf(
                        AdmissionQuantity(ticketTypeId = standardTicketTypeId(), quantity = 2),
                        AdmissionQuantity(ticketTypeId = vipTicketTypeId, quantity = 1)
                    )
                )
            )
        }

        assertTrue(exception.message!!.contains("Not enough admission capacity"))
        assertEquals(0, admissionHeld(generalAdmissionEvent().id, standardTicketTypeId()))
        assertEquals(1, admissionHeld(generalAdmissionEvent().id, vipTicketTypeId))
    }

    private fun insertSeat(eventId: UUID, seatKey: SeatKey, price: Int) {
        jdbcTemplate.update(
            """
            insert into event_seat_inventory (
                event_id, section_key, row_key, seat_number, price, status, hold_order_id, hold_expires_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            eventId,
            seatKey.sectionKey,
            seatKey.rowKey,
            seatKey.seatKey,
            price,
            SeatStatus.AVAILABLE.name,
            null,
            null
        )
    }

    private fun insertAdmissionInventory(eventId: UUID, ticketTypeId: UUID, price: Int, capacity: Int) {
        jdbcTemplate.update(
            """
            insert into event_admission_inventory (event_id, ticket_type_id, price, capacity, held, sold)
            values (?, ?, ?, ?, 0, 0)
            """.trimIndent(),
            eventId,
            ticketTypeId,
            price,
            capacity
        )
    }

    private fun seatStatus(eventId: UUID, seatKey: SeatKey): String = jdbcTemplate.queryForObject(
        """
        select status
        from event_seat_inventory
        where event_id = ? and section_key = ? and row_key = ? and seat_number = ?
        """.trimIndent(),
        String::class.java,
        eventId,
        seatKey.sectionKey,
        seatKey.rowKey,
        seatKey.seatKey
    )!!

    private fun heldOrderId(eventId: UUID, seatKey: SeatKey): UUID? = jdbcTemplate.query(
        """
        select hold_order_id
        from event_seat_inventory
        where event_id = ? and section_key = ? and row_key = ? and seat_number = ?
        """.trimIndent(),
        { rs, _ -> rs.getString("hold_order_id")?.let(UUID::fromString) },
        eventId,
        seatKey.sectionKey,
        seatKey.rowKey,
        seatKey.seatKey
    ).single()

    private fun admissionHeld(eventId: UUID, ticketTypeId: UUID): Int = jdbcTemplate.queryForObject(
        "select held from event_admission_inventory where event_id = ? and ticket_type_id = ?",
        Int::class.java,
        eventId,
        ticketTypeId
    )!!

    private fun admissionSold(eventId: UUID, ticketTypeId: UUID): Int = jdbcTemplate.queryForObject(
        "select sold from event_admission_inventory where event_id = ? and ticket_type_id = ?",
        Int::class.java,
        eventId,
        ticketTypeId
    )!!

    private fun seat(number: Int): SeatKey = SeatKey("parter", "r1", number.toString())

    private fun standardTicketTypeId(): UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614179010")

    private fun seatedEvent(): Event = Event(
        label = "Hamlet",
        description = "Evening show",
        venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614179001"),
        categoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614179002"),
        time = Instant.parse("2026-04-01T18:00:00Z"),
        venueSpaceId = UUID.fromString("123e4567-e89b-12d3-a456-426614179003"),
        id = UUID.fromString("123e4567-e89b-12d3-a456-426614179004"),
        organizationId = organization().id
    )

    private fun generalAdmissionEvent(): Event = Event(
        label = "Festival",
        description = "Open floor event",
        venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614179011"),
        categoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614179012"),
        time = Instant.parse("2026-04-01T20:00:00Z"),
        venueSpaceId = null,
        id = UUID.fromString("123e4567-e89b-12d3-a456-426614179013"),
        organizationId = organization().id
    )

    private fun organization(): Organization = Organization(
        code = "demo-org",
        name = "Demo Org",
        id = UUID.fromString("123e4567-e89b-12d3-a456-426614179020")
    )

    private fun buyer(): User = User(
        email = "buyer@example.com",
        fullName = "Buyer",
        id = UUID.fromString("123e4567-e89b-12d3-a456-426614179021")
    )
}

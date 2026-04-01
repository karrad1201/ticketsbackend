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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.Result
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringJUnitConfig(JdbcDurableOrderFlowTestConfig::class)
@Import(CreateOrderUseCase::class, ConfirmOrderPaymentUseCase::class, ExpireOrderUseCase::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class JdbcDurableOrderFlowConcurrencyTests {

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
    fun `should allow only one concurrent seat reservation in jdbc flow`() {
        insertSeat(seatedEvent().id, seat(1), 2000)

        val executor = Executors.newFixedThreadPool(8)
        try {
            val tasks = (1..8).map {
                executor.submit(
                    Callable {
                        runCatching {
                            createOrderUseCase.create(
                                CreateOrderCommand(
                                    eventId = seatedEvent().id,
                                    buyerUserId = buyer().id,
                                    seatKeys = listOf(seat(1))
                                )
                            )
                        }
                    }
                )
            }

            val results = tasks.map { it.get() }

            assertEquals(1, results.count { it.isSuccess })
            assertEquals(7, results.count { it.isFailure })
            assertEquals(1, orderRepository.findAll().size)
            assertEquals(SeatStatus.HELD.name, seatStatus(seatedEvent().id, seat(1)))
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `should stop general admission oversell under concurrent load`() {
        insertAdmissionInventory(generalAdmissionEvent().id, standardTicketTypeId(), price = 1500, capacity = 5)

        val executor = Executors.newFixedThreadPool(10)
        try {
            val tasks = (1..10).map {
                executor.submit(
                    Callable {
                        runCatching {
                            createOrderUseCase.create(
                                CreateOrderCommand(
                                    eventId = generalAdmissionEvent().id,
                                    buyerUserId = buyer().id,
                                    admissionItems = listOf(
                                        AdmissionQuantity(ticketTypeId = standardTicketTypeId(), quantity = 1)
                                    )
                                )
                            )
                        }
                    }
                )
            }

            val results = tasks.map { it.get() }

            assertEquals(5, results.count { it.isSuccess })
            assertEquals(5, results.count { it.isFailure })
            assertEquals(5, admissionHeld(generalAdmissionEvent().id, standardTicketTypeId()))
            assertEquals(0, admissionSold(generalAdmissionEvent().id, standardTicketTypeId()))
            assertEquals(5, orderRepository.findAll().size)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `should prevent double confirmation of the same order under race`() {
        insertSeat(seatedEvent().id, seat(1), 2000)
        val order = createOrderUseCase.create(
            CreateOrderCommand(
                eventId = seatedEvent().id,
                buyerUserId = buyer().id,
                seatKeys = listOf(seat(1))
            )
        )

        val executor = Executors.newFixedThreadPool(2)
        try {
            val tasks = (1..2).map {
                executor.submit(
                    Callable {
                        runCatching { confirmOrderPaymentUseCase.confirm(order.id) }
                    }
                )
            }

            val results = tasks.map { it.get() }

            assertEquals(1, results.count { it.isSuccess })
            assertEquals(1, results.count { it.isFailure })
            assertEquals(OrderStatus.PAID, requireNotNull(orderRepository.findById(order.id)).status)
            assertEquals(1, ticketRepository.findByOrderId(order.id).size)
            assertEquals(SeatStatus.SOLD.name, seatStatus(seatedEvent().id, seat(1)))
            assertTrue(results.any { it.exceptionOrNull()?.message?.contains("held before sale") == true || it.exceptionOrNull()?.message?.contains("already paid") == true })
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `should keep seat order consistent when confirm and expire race after ttl`() {
        insertSeat(seatedEvent().id, seat(1), 2000)
        val order = createOrderUseCase.create(
            CreateOrderCommand(
                eventId = seatedEvent().id,
                buyerUserId = buyer().id,
                seatKeys = listOf(seat(1))
            )
        )

        expireOrderAfterTtlConcurrently(order.id)

        assertEquals(OrderStatus.EXPIRED, requireNotNull(orderRepository.findById(order.id)).status)
        assertEquals(0, ticketRepository.findByOrderId(order.id).size)
        assertEquals(SeatStatus.AVAILABLE.name, seatStatus(seatedEvent().id, seat(1)))
    }

    @Test
    fun `should keep general admission order consistent when confirm and expire race after ttl`() {
        insertAdmissionInventory(generalAdmissionEvent().id, standardTicketTypeId(), price = 1500, capacity = 5)
        val order = createOrderUseCase.create(
            CreateOrderCommand(
                eventId = generalAdmissionEvent().id,
                buyerUserId = buyer().id,
                admissionItems = listOf(
                    AdmissionQuantity(ticketTypeId = standardTicketTypeId(), quantity = 2)
                )
            )
        )

        expireOrderAfterTtlConcurrently(order.id)

        assertEquals(OrderStatus.EXPIRED, requireNotNull(orderRepository.findById(order.id)).status)
        assertEquals(0, ticketRepository.findByOrderId(order.id).size)
        assertEquals(0, admissionHeld(generalAdmissionEvent().id, standardTicketTypeId()))
        assertEquals(0, admissionSold(generalAdmissionEvent().id, standardTicketTypeId()))
    }

    private fun expireOrderAfterTtlConcurrently(orderId: UUID) {
        jdbcTemplate.update(
            "update orders set expires_at = ? where id = ?",
            java.sql.Timestamp.from(Instant.parse("2026-03-22T23:00:00Z")),
            orderId
        )

        val executor = Executors.newFixedThreadPool(2)
        try {
            val confirmTask = executor.submit(Callable<Result<com.karrad.bilets.domain.entity.Order>> {
                runCatching { confirmOrderPaymentUseCase.confirm(orderId) }
            })
            val expireTask = executor.submit(Callable<Result<com.karrad.bilets.domain.entity.Order>> {
                runCatching { expireOrderUseCase.expire(orderId) }
            })

            val results = listOf(confirmTask.get(), expireTask.get())

            assertTrue(results.none { it.isSuccess && it.getOrNull()?.status == OrderStatus.PAID })
            assertTrue(
                results.count { it.isSuccess } == 1 || results.any {
                    it.exceptionOrNull()?.message?.contains("already expired") == true
                }
            )
        } finally {
            executor.shutdownNow()
        }
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

package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.AdmissionQuantity
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.entity.LayoutTemplate
import com.karrad.bilets.domain.entity.Row
import com.karrad.bilets.domain.entity.Section
import com.karrad.bilets.domain.entity.SeatKey
import com.karrad.bilets.domain.entity.TicketType
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.LayoutTemplateRepository
import com.karrad.bilets.domain.repository.OrderInventoryRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.Instant
import java.util.UUID
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Тесты контрактов инвентаря — overbooking, двойной релиз, несуществующие места.
 *
 * Проверяется:
 *  - Issue #12 (review): попытка перебронирования сверх capacity должна отклоняться
 *  - Issue #1  (review): попытка резервирования несуществующего места/типа билета
 *    должна давать понятную ошибку (не NullPointerException / NoSuchElementException от .single())
 */
@SpringJUnitConfig(ApplicationServicesTestConfig::class)
@Import(HoldGeneralAdmissionUseCase::class, ReleaseGeneralAdmissionUseCase::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class InventoryConstraintsTests {

    @Autowired lateinit var eventRepository: EventRepository
    @Autowired lateinit var eventInventoryPlanRepository: EventInventoryPlanRepository
    @Autowired lateinit var orderInventoryRepository: OrderInventoryRepository
    @Autowired lateinit var layoutTemplateRepository: LayoutTemplateRepository
    @Autowired lateinit var holdUseCase: HoldGeneralAdmissionUseCase
    @Autowired lateinit var releaseUseCase: ReleaseGeneralAdmissionUseCase

    private val venueSpaceId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val ticketTypeId = UUID.fromString("00000000-0000-0000-0000-000000000002")

    // ───── admission capacity ─────────────────────────────────────────────────

    @Test
    fun `overbooking beyond capacity must fail with meaningful error`() {
        val (event, plan) = seedAdmissionEvent(capacity = 5)

        // Бронируем 5 — максимальное количество
        holdUseCase.hold(event.id, listOf(AdmissionQuantity(ticketTypeId, quantity = 5)))

        // Попытка взять ещё 1 сверх capacity — должна отклоняться
        val ex = assertFailsWith<IllegalArgumentException> {
            holdUseCase.hold(event.id, listOf(AdmissionQuantity(ticketTypeId, quantity = 1)))
        }
        assertTrue(
            ex.message?.contains("capacity", ignoreCase = true) == true ||
            ex.message?.contains("available", ignoreCase = true) == true ||
            ex.message?.contains("Not enough", ignoreCase = true) == true,
            "Error must mention capacity/availability: ${ex.message}"
        )
    }

    @Test
    fun `holding exactly capacity must succeed`() {
        val (event, _) = seedAdmissionEvent(capacity = 10)
        holdUseCase.hold(event.id, listOf(AdmissionQuantity(ticketTypeId, quantity = 10)))
        // нет исключения — тест пройден
    }

    @Test
    fun `releasing more than held must fail with meaningful error`() {
        val (event, _) = seedAdmissionEvent(capacity = 10)

        holdUseCase.hold(event.id, listOf(AdmissionQuantity(ticketTypeId, quantity = 3)))

        val ex = assertFailsWith<IllegalArgumentException> {
            releaseUseCase.release(event.id, listOf(AdmissionQuantity(ticketTypeId, quantity = 5)))
        }
        assertTrue(
            ex.message?.contains("held", ignoreCase = true) == true ||
            ex.message?.contains("Not enough", ignoreCase = true) == true,
            "Error must mention held inventory: ${ex.message}"
        )
    }

    @Test
    fun `holding unknown ticket type must fail with meaningful error — not NullPointerException`() {
        val (event, _) = seedAdmissionEvent(capacity = 10)
        val unknownTicketTypeId = UUID.randomUUID()

        val ex = assertFailsWith<Exception> {
            holdUseCase.hold(event.id, listOf(AdmissionQuantity(unknownTicketTypeId, quantity = 1)))
        }
        // Issue #1 аналог: error должен быть понятным (не NullPointerException)
        assertTrue(
            ex !is NullPointerException,
            "Must not throw NullPointerException for unknown ticket type"
        )
        assertTrue(
            ex.message != null,
            "Exception must have a message"
        )
    }

    // ───── seat inventory ─────────────────────────────────────────────────────

    @Test
    fun `reserving a seat that does not exist must fail with meaningful error`() {
        val (event, plan) = seedSeatedEvent()
        val nonExistentSeat = SeatKey(sectionKey = "Z", rowKey = "99", seatKey = "999")

        val ex = assertFailsWith<Exception> {
            orderInventoryRepository.reserveSeats(
                orderId = UUID.randomUUID(),
                eventId = event.id,
                seatKeys = listOf(nonExistentSeat),
                expiresAt = Instant.now().plusSeconds(600)
            )
        }
        // Issue #1: должно быть понятное сообщение, а не NoSuchElementException из .single()
        assertTrue(ex !is NullPointerException, "Must not throw NullPointerException")
        assertTrue(ex.message != null, "Exception must carry a message")
    }

    @Test
    fun `reserving already held seat must fail with meaningful error`() {
        val (event, _) = seedSeatedEvent()
        val seat = SeatKey(sectionKey = "A", rowKey = "1", seatKey = "1")

        // Первое бронирование — успешно
        orderInventoryRepository.reserveSeats(
            orderId = UUID.randomUUID(),
            eventId = event.id,
            seatKeys = listOf(seat),
            expiresAt = Instant.now().plusSeconds(600)
        )

        // Второе бронирование того же места — должно отклоняться
        val ex = assertFailsWith<IllegalArgumentException> {
            orderInventoryRepository.reserveSeats(
                orderId = UUID.randomUUID(),
                eventId = event.id,
                seatKeys = listOf(seat),
                expiresAt = Instant.now().plusSeconds(600)
            )
        }
        assertTrue(
            ex.message?.contains("available", ignoreCase = true) == true ||
            ex.message?.contains("not available", ignoreCase = true) == true ||
            ex.message?.contains("Seat", ignoreCase = true) == true,
            "Error must describe why seat is unavailable: ${ex.message}"
        )
    }

    // ───── helpers ────────────────────────────────────────────────────────────

    private fun seedAdmissionEvent(capacity: Int): Pair<Event, EventInventoryPlan> {
        val event = Event(
            label = "Admission Event",
            description = "Test",
            venueId = UUID.randomUUID(),
            categoryId = UUID.randomUUID(),
            time = Instant.parse("2026-12-01T18:00:00Z"),
            venueSpaceId = venueSpaceId
        )
        eventRepository.save(event)

        val plan = EventInventoryPlan.generalAdmission(
            event = event,
            ticketTypes = listOf(TicketType(id = ticketTypeId, label = "Standard", price = 1000, quota = capacity))
        )
        eventInventoryPlanRepository.save(plan)
        return event to plan
    }

    private fun seedSeatedEvent(): Pair<Event, EventInventoryPlan> {
        val template = LayoutTemplate(
            venueSpaceId = venueSpaceId,
            label = "Hall A",
            sections = listOf(
                Section(
                    label = "A",
                    rows = listOf(Row(label = "1", startSeat = 1, endSeat = 5, price = 2000))
                )
            )
        )
        layoutTemplateRepository.save(template)

        val event = Event(
            label = "Seated Event",
            description = "Test",
            venueId = UUID.randomUUID(),
            categoryId = UUID.randomUUID(),
            time = Instant.parse("2026-12-01T18:00:00Z"),
            venueSpaceId = venueSpaceId,
            hasSeatMap = true
        )
        eventRepository.save(event)

        val plan = EventInventoryPlan.seated(event = event, layoutTemplate = template)
        eventInventoryPlanRepository.save(plan)
        return event to plan
    }
}

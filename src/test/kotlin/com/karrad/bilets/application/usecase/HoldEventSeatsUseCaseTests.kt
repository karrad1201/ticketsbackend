package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.LayoutTemplate
import com.karrad.bilets.domain.entity.Row
import com.karrad.bilets.domain.entity.SeatKey
import com.karrad.bilets.domain.entity.Section
import com.karrad.bilets.domain.entity.TicketType
import com.karrad.bilets.domain.enums.SeatStatus
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.LayoutTemplateRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@SpringJUnitConfig(ApplicationServicesTestConfig::class)
@Import(HoldEventSeatsUseCase::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class HoldEventSeatsUseCaseTests {

    @Autowired
    lateinit var eventRepository: EventRepository

    @Autowired
    lateinit var layoutTemplateRepository: LayoutTemplateRepository

    @Autowired
    lateinit var eventInventoryPlanRepository: EventInventoryPlanRepository

    @Autowired
    lateinit var useCase: HoldEventSeatsUseCase

    @Test
    fun `should hold requested available seats`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        layoutTemplateRepository.save(layoutTemplate)
        val plan = eventInventoryPlanRepository.save(
            com.karrad.bilets.domain.entity.EventInventoryPlan.seated(event, layoutTemplate)
        )

        val result = useCase.hold(
            eventId = event.id,
            seatKeys = listOf(
                SeatKey(sectionKey = "parter", rowKey = "r1", seatKey = "1"),
                SeatKey(sectionKey = "parter", rowKey = "r1", seatKey = "2")
            )
        )

        assertEquals(SeatStatus.HELD, result.seatInventory.first { it.seatNumber == "1" }.status)
        assertEquals(SeatStatus.HELD, result.seatInventory.first { it.seatNumber == "2" }.status)
        assertEquals(SeatStatus.AVAILABLE, result.seatInventory.first { it.seatNumber == "3" }.status)
        assertEquals(result, eventInventoryPlanRepository.findByEventId(plan.eventId))
    }

    @Test
    fun `should reject hold when inventory plan does not exist`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.hold(
                eventId = UUID.fromString("123e4567-e89b-12d3-a456-426614174701"),
                seatKeys = listOf(SeatKey(sectionKey = "parter", rowKey = "r1", seatKey = "1"))
            )
        }

        assertTrue(exception.message!!.contains("EventInventoryPlan not found"))
    }

    @Test
    fun `should reject hold when seat does not exist`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        layoutTemplateRepository.save(layoutTemplate)
        eventInventoryPlanRepository.save(
            com.karrad.bilets.domain.entity.EventInventoryPlan.seated(event, layoutTemplate)
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.hold(
                eventId = event.id,
                seatKeys = listOf(SeatKey(sectionKey = "parter", rowKey = "r1", seatKey = "99"))
            )
        }

        assertTrue(exception.message!!.contains("Seats not found"))
    }

    @Test
    fun `should reject hold when seat is already unavailable`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        layoutTemplateRepository.save(layoutTemplate)
        val heldPlan = com.karrad.bilets.domain.entity.EventInventoryPlan.seated(event, layoutTemplate).copy(
            seatInventory = com.karrad.bilets.domain.entity.EventInventoryPlan.seated(event, layoutTemplate)
                .seatInventory
                .map {
                    if (it.seatNumber == "1") it.copy(status = SeatStatus.HELD) else it
                }
        )
        eventInventoryPlanRepository.save(heldPlan)

        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.hold(
                eventId = event.id,
                seatKeys = listOf(SeatKey(sectionKey = "parter", rowKey = "r1", seatKey = "1"))
            )
        }

        assertTrue(exception.message!!.contains("Seats are not available"))
    }

    @Test
    fun `should reject hold for general admission inventory`() {
        val event = generalAdmissionEvent()
        eventRepository.save(event)
        eventInventoryPlanRepository.save(
            com.karrad.bilets.domain.entity.EventInventoryPlan.generalAdmission(
                event = event,
                ticketTypes = listOf(TicketType(label = "Standard", price = 1500, quota = 100))
            )
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.hold(
                eventId = event.id,
                seatKeys = listOf(SeatKey(sectionKey = "parter", rowKey = "r1", seatKey = "1"))
            )
        }

        assertTrue(exception.message!!.contains("only for seated inventory"))
    }

    private fun seatedEvent(): Event {
        return Event(
            label = "Hamlet",
            description = "Evening show",
            venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614174710"),
            categoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174716"),
            time = Instant.parse("2026-04-01T18:00:00Z"),
            venueSpaceId = UUID.fromString("123e4567-e89b-12d3-a456-426614174711"),
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174712")
        )
    }

    private fun generalAdmissionEvent(): Event {
        return Event(
            label = "Festival",
            description = "Open floor event",
            venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614174713"),
            categoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174717"),
            time = Instant.parse("2026-04-01T18:00:00Z"),
            venueSpaceId = null,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174714")
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
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174715")
        )
    }
}

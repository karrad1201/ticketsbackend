package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.entity.LayoutTemplate
import com.karrad.bilets.domain.entity.Row
import com.karrad.bilets.domain.entity.SeatKey
import com.karrad.bilets.domain.entity.Section
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
@Import(SellEventSeatsUseCase::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SellEventSeatsUseCaseTests {

    @Autowired
    lateinit var eventRepository: EventRepository

    @Autowired
    lateinit var layoutTemplateRepository: LayoutTemplateRepository

    @Autowired
    lateinit var eventInventoryPlanRepository: EventInventoryPlanRepository

    @Autowired
    lateinit var useCase: SellEventSeatsUseCase

    @Test
    fun `should sell held seats`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        layoutTemplateRepository.save(layoutTemplate)
        val heldPlan = EventInventoryPlan.seated(event, layoutTemplate).holdSeats(
            listOf(SeatKey(sectionKey = "parter", rowKey = "r1", seatNumber = 1))
        )
        eventInventoryPlanRepository.save(heldPlan)

        val result = useCase.sell(
            eventId = event.id,
            seatKeys = listOf(SeatKey(sectionKey = "parter", rowKey = "r1", seatNumber = 1))
        )

        assertEquals(SeatStatus.SOLD, result.seatInventory.first { it.seatNumber == 1 }.status)
    }

    @Test
    fun `should reject sale when seat is not held`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        layoutTemplateRepository.save(layoutTemplate)
        eventInventoryPlanRepository.save(EventInventoryPlan.seated(event, layoutTemplate))

        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.sell(
                eventId = event.id,
                seatKeys = listOf(SeatKey(sectionKey = "parter", rowKey = "r1", seatNumber = 1))
            )
        }

        assertTrue(exception.message!!.contains("must be held before sale"))
    }

    private fun seatedEvent(): Event {
        return Event(
            label = "Hamlet",
            description = "Evening show",
            venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614174820"),
            categoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174824"),
            time = Instant.parse("2026-04-01T18:00:00Z"),
            venueSpaceId = UUID.fromString("123e4567-e89b-12d3-a456-426614174821"),
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174822")
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
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174823")
        )
    }
}

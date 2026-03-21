package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.AdmissionQuantity
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.entity.TicketType
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.EventRepository
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
@Import(HoldGeneralAdmissionUseCase::class, ReleaseGeneralAdmissionUseCase::class, SellGeneralAdmissionUseCase::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GeneralAdmissionInventoryLifecycleUseCaseTests {

    @Autowired
    lateinit var eventRepository: EventRepository

    @Autowired
    lateinit var eventInventoryPlanRepository: EventInventoryPlanRepository

    @Autowired
    lateinit var holdUseCase: HoldGeneralAdmissionUseCase

    @Autowired
    lateinit var releaseUseCase: ReleaseGeneralAdmissionUseCase

    @Autowired
    lateinit var sellUseCase: SellGeneralAdmissionUseCase

    @Test
    fun `should hold general admission capacity`() {
        val event = generalAdmissionEvent()
        eventRepository.save(event)
        eventInventoryPlanRepository.save(generalAdmissionPlan(event))

        val result = holdUseCase.hold(
            event.id,
            listOf(AdmissionQuantity(ticketTypeId = standardTicketTypeId(), quantity = 5))
        )

        val inventory = result.admissionInventory.first { it.ticketTypeId == standardTicketTypeId() }
        assertEquals(5, inventory.held)
        assertEquals(95, inventory.available)
    }

    @Test
    fun `should release held admission capacity`() {
        val event = generalAdmissionEvent()
        eventRepository.save(event)
        val heldPlan = generalAdmissionPlan(event).holdAdmission(
            listOf(AdmissionQuantity(ticketTypeId = standardTicketTypeId(), quantity = 5))
        )
        eventInventoryPlanRepository.save(heldPlan)

        val result = releaseUseCase.release(
            event.id,
            listOf(AdmissionQuantity(ticketTypeId = standardTicketTypeId(), quantity = 3))
        )

        val inventory = result.admissionInventory.first { it.ticketTypeId == standardTicketTypeId() }
        assertEquals(2, inventory.held)
        assertEquals(98, inventory.available)
    }

    @Test
    fun `should sell held admission capacity`() {
        val event = generalAdmissionEvent()
        eventRepository.save(event)
        val heldPlan = generalAdmissionPlan(event).holdAdmission(
            listOf(AdmissionQuantity(ticketTypeId = standardTicketTypeId(), quantity = 5))
        )
        eventInventoryPlanRepository.save(heldPlan)

        val result = sellUseCase.sell(
            event.id,
            listOf(AdmissionQuantity(ticketTypeId = standardTicketTypeId(), quantity = 4))
        )

        val inventory = result.admissionInventory.first { it.ticketTypeId == standardTicketTypeId() }
        assertEquals(1, inventory.held)
        assertEquals(4, inventory.sold)
        assertEquals(95, inventory.available)
    }

    @Test
    fun `should reject admission hold when capacity is insufficient`() {
        val event = generalAdmissionEvent()
        eventRepository.save(event)
        eventInventoryPlanRepository.save(generalAdmissionPlan(event))

        val exception = assertFailsWith<IllegalArgumentException> {
            holdUseCase.hold(
                event.id,
                listOf(AdmissionQuantity(ticketTypeId = standardTicketTypeId(), quantity = 101))
            )
        }

        assertTrue(exception.message!!.contains("Not enough admission capacity"))
    }

    private fun generalAdmissionEvent(): Event {
        return Event(
            label = "Festival",
            description = "Open floor event",
            venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614174901"),
            categoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174905"),
            time = Instant.parse("2026-04-01T18:00:00Z"),
            venueSpaceId = null,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174902")
        )
    }

    private fun generalAdmissionPlan(event: Event): EventInventoryPlan {
        return EventInventoryPlan.generalAdmission(
            event = event,
            ticketTypes = listOf(
                TicketType(label = "Standard", price = 1500, quota = 100, id = standardTicketTypeId()),
                TicketType(label = "VIP", price = 3000, quota = 20, id = UUID.fromString("123e4567-e89b-12d3-a456-426614174903"))
            )
        )
    }

    private fun standardTicketTypeId(): UUID =
        UUID.fromString("123e4567-e89b-12d3-a456-426614174904")
}

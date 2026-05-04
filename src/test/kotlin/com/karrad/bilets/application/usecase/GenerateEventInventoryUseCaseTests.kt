package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.LayoutTemplate
import com.karrad.bilets.domain.entity.Row
import com.karrad.bilets.domain.entity.Section
import com.karrad.bilets.domain.entity.TicketType
import com.karrad.bilets.domain.entity.InventoryMode
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.LayoutTemplateRepository
import com.karrad.bilets.domain.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringJUnitConfig(ApplicationServicesTestConfig::class)
@Import(GenerateEventInventoryUseCase::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GenerateEventInventoryUseCaseTests {

    @Autowired
    lateinit var eventRepository: EventRepository

    @Autowired
    lateinit var layoutTemplateRepository: LayoutTemplateRepository

    @Autowired
    lateinit var eventInventoryPlanRepository: EventInventoryPlanRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var useCase: GenerateEventInventoryUseCase

    private val adminId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")

    @BeforeEach
    fun setUpAdmin() {
        userRepository.save(User(id = adminId, fullName = "Test Admin", phone = "+70000000001", role = UserRole.ADMIN))
    }

    @Test
    fun `should generate seated inventory plan and persist it`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        layoutTemplateRepository.save(layoutTemplate)

        val result = useCase.generateSeated(
            eventId = event.id,
            layoutTemplateId = layoutTemplate.id,
            callerUserId = adminId
        )

        assertEquals(InventoryMode.SEATED, result.mode)
        assertEquals(3, result.seatInventory.size)
        assertEquals(result, eventInventoryPlanRepository.findByEventId(event.id))
    }

    @Test
    fun `should reject seated inventory generation when event does not exist`() {
        val layoutTemplate = seatedLayoutTemplate(UUID.fromString("123e4567-e89b-12d3-a456-426614174201"))
        layoutTemplateRepository.save(layoutTemplate)

        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.generateSeated(
                eventId = UUID.fromString("123e4567-e89b-12d3-a456-426614174202"),
                layoutTemplateId = layoutTemplate.id,
                callerUserId = adminId
            )
        }

        assertTrue(exception.message!!.contains("Event not found"))
    }

    @Test
    fun `should reject seated inventory generation when layout does not exist`() {
        val event = seatedEvent()
        eventRepository.save(event)

        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.generateSeated(
                eventId = event.id,
                layoutTemplateId = UUID.fromString("123e4567-e89b-12d3-a456-426614174203"),
                callerUserId = adminId
            )
        }

        assertTrue(exception.message!!.contains("LayoutTemplate not found"))
    }

    @Test
    fun `should reject inventory generation when plan already exists`() {
        val event = seatedEvent()
        val layoutTemplate = seatedLayoutTemplate(requireNotNull(event.venueSpaceId))
        eventRepository.save(event)
        layoutTemplateRepository.save(layoutTemplate)

        useCase.generateSeated(event.id, layoutTemplate.id, adminId)

        val exception = assertFailsWith<IllegalStateException> {
            useCase.generateSeated(event.id, layoutTemplate.id, adminId)
        }

        assertTrue(exception.message!!.contains("already exists"))
    }

    @Test
    fun `should generate general admission inventory plan and persist it`() {
        val event = generalAdmissionEvent()
        eventRepository.save(event)

        val result = useCase.generateGeneralAdmission(
            eventId = event.id,
            ticketTypes = listOf(
                TicketType(label = "Standard", price = 1500, quota = 100),
                TicketType(label = "VIP", price = 3000, quota = 20)
            ),
            callerUserId = adminId
        )

        assertEquals(InventoryMode.GENERAL_ADMISSION, result.mode)
        assertEquals(2, result.admissionInventory.size)
        assertEquals(result, eventInventoryPlanRepository.findByEventId(event.id))
    }

    @Test
    fun `should reject general admission inventory generation when event does not exist`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.generateGeneralAdmission(
                eventId = UUID.fromString("123e4567-e89b-12d3-a456-426614174204"),
                ticketTypes = listOf(TicketType(label = "Standard", price = 1500, quota = 100)),
                callerUserId = adminId
            )
        }

        assertTrue(exception.message!!.contains("Event not found"))
    }

    @Test
    fun `should persist generated plan only once`() {
        val event = generalAdmissionEvent()
        eventRepository.save(event)

        val result = useCase.generateGeneralAdmission(
            eventId = event.id,
            ticketTypes = listOf(TicketType(label = "Standard", price = 1500, quota = 100)),
            callerUserId = adminId
        )

        val stored = eventInventoryPlanRepository.findByEventId(event.id)

        assertNotNull(stored)
        assertEquals(result, stored)
        assertEquals(1, eventInventoryPlanRepository.findAll().size)
    }

    private fun seatedEvent(): Event {
        return Event(
            label = "Hamlet",
            description = "Evening show",
            venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614174210"),
            categoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174216"),
            time = Instant.parse("2026-04-01T18:00:00Z"),
            venueSpaceId = UUID.fromString("123e4567-e89b-12d3-a456-426614174211"),
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174212")
        )
    }

    private fun generalAdmissionEvent(): Event {
        return Event(
            label = "Festival",
            description = "Open floor event",
            venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614174213"),
            categoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174217"),
            time = Instant.parse("2026-04-01T18:00:00Z"),
            venueSpaceId = null,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174214")
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
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174215")
        )
    }
}

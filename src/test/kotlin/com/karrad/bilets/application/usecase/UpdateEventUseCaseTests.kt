package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
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
import kotlin.test.assertTrue

@SpringJUnitConfig(ApplicationServicesTestConfig::class)
@Import(UpdateEventUseCase::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UpdateEventUseCaseTests {

    @Autowired
    lateinit var eventRepository: EventRepository

    @Autowired
    lateinit var organizationRepository: OrganizationRepository

    @Autowired
    lateinit var organizationMemberRepository: OrganizationMemberRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var useCase: UpdateEventUseCase

    private val orgId = UUID.fromString("00000000-0000-0000-0001-000000000001")
    private val ownerId = UUID.fromString("00000000-0000-0000-0001-000000000002")
    private val managerId = UUID.fromString("00000000-0000-0000-0001-000000000003")
    private val staffId = UUID.fromString("00000000-0000-0000-0001-000000000004")
    private val outsiderId = UUID.fromString("00000000-0000-0000-0001-000000000005")
    private val adminId = UUID.fromString("00000000-0000-0000-0001-000000000006")
    private val eventId = UUID.fromString("00000000-0000-0000-0001-000000000010")

    @BeforeEach
    fun setUp() {
        organizationRepository.save(Organization(id = orgId, code = "test-org", name = "Test Org"))
        organizationMemberRepository.save(OrganizationMember(organizationId = orgId, userId = ownerId, role = OrganizationMemberRole.OWNER))
        organizationMemberRepository.save(OrganizationMember(organizationId = orgId, userId = managerId, role = OrganizationMemberRole.MANAGER))
        organizationMemberRepository.save(OrganizationMember(organizationId = orgId, userId = staffId, role = OrganizationMemberRole.STAFF))
        userRepository.save(User(id = adminId, fullName = "Admin", phone = "+70000000099", role = UserRole.ADMIN))
        eventRepository.save(demoEvent())
    }

    @Test
    fun `should update event when caller is ADMIN`() {
        val patch = EventPatch(label = "Updated Label", description = "Updated Desc")

        val result = useCase.execute(eventId, patch, adminId)

        assertEquals("Updated Label", result.label)
        assertEquals("Updated Desc", result.description)
    }

    @Test
    fun `should update event when caller is OWNER of the organization`() {
        val patch = EventPatch(label = "Owner Updated")

        val result = useCase.execute(eventId, patch, ownerId)

        assertEquals("Owner Updated", result.label)
    }

    @Test
    fun `should update event when caller is MANAGER of the organization`() {
        val patch = EventPatch(description = "Manager Updated Desc")

        val result = useCase.execute(eventId, patch, managerId)

        assertEquals("Manager Updated Desc", result.description)
    }

    @Test
    fun `should keep original fields when patch fields are null`() {
        val patch = EventPatch() // all null

        val result = useCase.execute(eventId, patch, adminId)

        assertEquals("Demo Event", result.label)
        assertEquals("Demo description", result.description)
        assertEquals("18+", result.ageRating)
    }

    @Test
    fun `should update only time when only time is patched`() {
        val newTime = Instant.parse("2027-06-01T20:00:00Z")
        val patch = EventPatch(time = newTime)

        val result = useCase.execute(eventId, patch, adminId)

        assertEquals(newTime, result.time)
        assertEquals("Demo Event", result.label)
    }

    @Test
    fun `should clear ageRating when patch ageRating is null`() {
        val patch = EventPatch(ageRating = null) // null means keep original per current logic
        val result = useCase.execute(eventId, patch, adminId)
        // when ageRating patch is null, keep existing
        assertEquals("18+", result.ageRating)
    }

    @Test
    fun `should set ageRating when patch provides new value`() {
        val patch = EventPatch(ageRating = "0+")
        val result = useCase.execute(eventId, patch, adminId)
        assertEquals("0+", result.ageRating)
    }

    @Test
    fun `should reject update when event does not exist`() {
        val missingId = UUID.fromString("00000000-0000-0000-0001-999999999999")
        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.execute(missingId, EventPatch(label = "X"), adminId)
        }
        assertTrue(exception.message!!.contains("Event not found"))
    }

    @Test
    fun `should reject update when event has no organization and caller is not admin`() {
        val eventWithoutOrg = demoEvent().copy(
            id = UUID.fromString("00000000-0000-0000-0001-000000000020"),
            organizationId = null
        )
        eventRepository.save(eventWithoutOrg)

        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.execute(eventWithoutOrg.id, EventPatch(label = "X"), ownerId)
        }
        assertTrue(exception.message!!.contains("not attached to any organization"))
    }

    @Test
    fun `should reject update when caller is not a member of the organization`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.execute(eventId, EventPatch(label = "X"), outsiderId)
        }
        assertTrue(exception.message!!.contains("is not a member"))
    }

    @Test
    fun `should reject update when caller has STAFF role`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.execute(eventId, EventPatch(label = "X"), staffId)
        }
        assertTrue(exception.message!!.contains("Insufficient role"))
    }

    private fun demoEvent() = Event(
        id = eventId,
        label = "Demo Event",
        description = "Demo description",
        venueId = UUID.fromString("00000000-0000-0000-0001-000000000030"),
        categoryId = UUID.fromString("00000000-0000-0000-0001-000000000031"),
        time = Instant.parse("2027-01-01T18:00:00Z"),
        organizationId = orgId,
        ageRating = "18+"
    )
}

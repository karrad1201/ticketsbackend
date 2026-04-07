package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.VenueRepository
import com.karrad.bilets.support.MutableClock
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringJUnitConfig(ApplicationServicesTestConfig::class)
@Import(GetMyOrganizationEventsUseCase::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GetMyOrganizationEventsUseCaseTests {

    @Autowired lateinit var useCase: GetMyOrganizationEventsUseCase
    @Autowired lateinit var eventRepository: EventRepository
    @Autowired lateinit var organizationRepository: OrganizationRepository
    @Autowired lateinit var organizationMemberRepository: OrganizationMemberRepository
    @Autowired lateinit var venueRepository: VenueRepository
    @Autowired lateinit var mutableClock: MutableClock

    private val orgId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001")
    private val managerId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002")
    private val outsiderId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000003")
    private val venueId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000004")

    private fun seed() {
        organizationRepository.save(Organization(code = "test-org", name = "Test Org", id = orgId))
        organizationMemberRepository.save(
            OrganizationMember(organizationId = orgId, userId = managerId, role = OrganizationMemberRole.OWNER)
        )
        venueRepository.save(
            Venue(
                label = "Test Venue",
                city = City(label = "Yekaterinburg", subject = Subject("Sverdlovsk Oblast")),
                organizationId = orgId,
                id = venueId
            )
        )
    }

    private fun event(label: String, time: Instant, id: UUID = UUID.randomUUID()) = Event(
        label = label,
        description = "desc",
        venueId = venueId,
        categoryId = UUID.randomUUID(),
        time = time,
        id = id,
        organizationId = orgId
    )

    @Test
    fun `should return upcoming events for manager's organization sorted by time`() {
        seed()
        // clock at 2026-03-23 (from ApplicationServicesTestConfig)
        val future1 = event("Concert A", Instant.parse("2026-04-10T18:00:00Z"))
        val future2 = event("Concert B", Instant.parse("2026-04-05T18:00:00Z"))
        val past = event("Past Show", Instant.parse("2026-03-01T18:00:00Z"))
        eventRepository.save(future1)
        eventRepository.save(future2)
        eventRepository.save(past)

        val result = useCase.execute(managerId)

        assertEquals(listOf("Concert B", "Concert A"), result.map { it.label })
    }

    @Test
    fun `should return empty list when user has no organization`() {
        val result = useCase.execute(outsiderId)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `should not return events of other organizations`() {
        seed()
        val otherOrgId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000099")
        eventRepository.save(
            event("Other Org Event", Instant.parse("2026-05-01T18:00:00Z")).copy(organizationId = otherOrgId)
        )
        eventRepository.save(event("My Event", Instant.parse("2026-04-15T18:00:00Z")))

        val result = useCase.execute(managerId)

        assertEquals(listOf("My Event"), result.map { it.label })
    }
}

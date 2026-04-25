package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.Ticket
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.TicketRepository
import com.karrad.bilets.domain.repository.UserRepository
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
import kotlin.test.assertIs

@SpringJUnitConfig(ApplicationServicesTestConfig::class)
@Import(ValidateTicketUseCase::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ValidateTicketUseCaseTests {

    @Autowired lateinit var useCase: ValidateTicketUseCase
    @Autowired lateinit var ticketRepository: TicketRepository
    @Autowired lateinit var eventRepository: EventRepository
    @Autowired lateinit var organizationRepository: OrganizationRepository
    @Autowired lateinit var organizationMemberRepository: OrganizationMemberRepository
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var venueRepository: VenueRepository
    @Autowired lateinit var mutableClock: MutableClock

    private val orgId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")
    private val eventId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002")
    private val otherEventId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000003")
    private val managerId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000004")
    private val buyerId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000005")
    private val outsiderId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000006")
    private val venueId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000007")

    private fun seed(): Ticket {
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
        eventRepository.save(
            Event(
                label = "Main Concert",
                description = "desc",
                venueId = venueId,
                categoryId = UUID.randomUUID(),
                time = Instant.parse("2026-06-01T18:00:00Z"),
                id = eventId,
                organizationId = orgId
            )
        )
        eventRepository.save(
            Event(
                label = "Other Concert",
                description = "desc",
                venueId = venueId,
                categoryId = UUID.randomUUID(),
                time = Instant.parse("2026-06-02T18:00:00Z"),
                id = otherEventId,
                organizationId = orgId
            )
        )
        userRepository.save(User(email = "buyer@test.com", fullName = "Ivan Ivanov", id = buyerId))

        val ticket = Ticket(
            orderId = UUID.randomUUID(),
            eventId = eventId,
            userId = buyerId,
            price = 1500,
            ticketTypeId = UUID.randomUUID(),
            issuedAt = Instant.parse("2026-05-01T10:00:00Z")
        )
        return ticketRepository.save(ticket)
    }

    @Test
    fun `should return Valid and mark ticket as used`() {
        val ticket = seed()
        // MutableClock starts at 2026-03-23T00:00:00Z by default (from ApplicationServicesTestConfig)
        val now = mutableClock.instant()

        val result = useCase.execute(ticket.id, eventId, managerId)

        assertIs<TicketValidationResult.Valid>(result)
        assertEquals(ticket.id, result.ticketId)
        assertEquals(eventId, result.eventId)
        assertEquals("Main Concert", result.eventLabel)
        assertEquals("Ivan Ivanov", result.holderName)
        assertEquals(now, result.usedAt)

        // повторное сканирование → AlreadyUsed
        val second = useCase.execute(ticket.id, eventId, managerId)
        assertIs<TicketValidationResult.AlreadyUsed>(second)
        assertEquals(now, second.usedAt)
    }

    @Test
    fun `should return WrongEvent when ticket belongs to different event`() {
        val ticket = seed()

        val result = useCase.execute(ticket.id, otherEventId, managerId)

        assertIs<TicketValidationResult.WrongEvent>(result)
        assertEquals(ticket.id, result.ticketId)
        assertEquals("Main Concert", result.ticketEventLabel)
        assertEquals("Other Concert", result.scannedEventLabel)
    }

    @Test
    fun `should return Unauthorized when caller is not org member`() {
        seed()

        val result = useCase.execute(UUID.randomUUID(), eventId, outsiderId)

        assertIs<TicketValidationResult.Unauthorized>(result)
    }

    @Test
    fun `should return NotFound when event does not exist`() {
        val result = useCase.execute(UUID.randomUUID(), UUID.randomUUID(), managerId)

        assertIs<TicketValidationResult.NotFound>(result)
    }

    @Test
    fun `should return NotFound when ticket does not exist`() {
        seed()

        val result = useCase.execute(UUID.randomUUID(), eventId, managerId)

        assertIs<TicketValidationResult.NotFound>(result)
    }

    @Test
    fun `should return AlreadyUsed when ticket was previously validated`() {
        val ticket = seed()
        val usedAt = Instant.parse("2026-06-01T16:00:00Z")
        ticketRepository.markAsUsed(ticket.id, usedAt)

        val result = useCase.execute(ticket.id, eventId, managerId)

        assertIs<TicketValidationResult.AlreadyUsed>(result)
        assertEquals(usedAt, result.usedAt)
    }

    @Test
    fun `should return Valid for STAFF assigned to the correct venue`() {
        val ticket = seed()
        val staffId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000010")
        organizationMemberRepository.save(
            OrganizationMember(organizationId = orgId, userId = staffId, role = OrganizationMemberRole.STAFF, venueId = venueId)
        )

        val result = useCase.execute(ticket.id, eventId, staffId)

        assertIs<TicketValidationResult.Valid>(result)
    }

    @Test
    fun `should return Unauthorized for STAFF assigned to a different venue`() {
        seed()
        val staffId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000011")
        val otherVenueId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000012")
        organizationMemberRepository.save(
            OrganizationMember(organizationId = orgId, userId = staffId, role = OrganizationMemberRole.STAFF, venueId = otherVenueId)
        )

        val result = useCase.execute(UUID.randomUUID(), eventId, staffId)

        assertIs<TicketValidationResult.Unauthorized>(result)
    }
}

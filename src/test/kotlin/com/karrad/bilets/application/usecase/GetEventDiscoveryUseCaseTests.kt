package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.UserEventVisit
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.VenueSpace
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.repository.CategoryRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.UserEventVisitRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.domain.repository.VenueRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@SpringJUnitConfig(ApplicationServicesTestConfig::class)
@Import(GetEventDiscoveryUseCase::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GetEventDiscoveryUseCaseTests {

    @Autowired
    lateinit var categoryRepository: CategoryRepository

    @Autowired
    lateinit var venueRepository: VenueRepository

    @Autowired
    lateinit var eventRepository: EventRepository

    @Autowired
    lateinit var userEventVisitRepository: UserEventVisitRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var useCase: GetEventDiscoveryUseCase

    @Test
    fun `should build discovery sections for city`() {
        seedCatalog()

        userEventVisitRepository.save(
            UserEventVisit(
                userId = userId(),
                eventId = UUID.fromString("123e4567-e89b-12d3-a456-426614176210"),
                visitedAt = Instant.now()
            )
        )
        userEventVisitRepository.save(
            UserEventVisit(
                userId = userId(),
                eventId = UUID.fromString("123e4567-e89b-12d3-a456-426614176211"),
                visitedAt = Instant.now()
            )
        )

        val result = useCase.get(userId = userId(), city = "Ekaterinburg", page = 0, size = 10)

        assertEquals(
            listOf("Rock Tomorrow", "Jazz Reunion", "Indie Day After Tomorrow"),
            result.forYou.map { it.label }
        )
        assertEquals(
            listOf("Jazz Reunion"),
            result.byCategory.find { it.category.code == "jazz" }?.events?.map { it.label }
        )
        assertEquals(
            listOf("Rock Tomorrow", "Indie Day After Tomorrow"),
            result.byCategory.find { it.category.code == "rock" }?.events?.map { it.label }
        )
        assertEquals(listOf("Rock Tomorrow"), result.tomorrow.map { it.label })
        assertEquals(listOf("Indie Day After Tomorrow"), result.dayAfterTomorrow.map { it.label })
    }

    @Test
    fun `should reject invalid discovery page size`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.get(userId = userId(), city = "Ekaterinburg", page = 0, size = 51)
        }

        assertEquals("size must be between 1 and 50", exception.message)
    }

    @Test
    fun `should boost events matching user interests in forYou`() {
        seedCatalog()
        // user with "jazz" interest → jazz events get interestScore bonus
        userRepository.save(
            User(fullName = "Jazz Fan", phone = "+70000000001", interests = listOf("jazz"), id = userId())
        )

        val result = useCase.get(userId = userId(), city = "Ekaterinburg", page = 0, size = 10)

        // Jazz Reunion should be boosted; all future events should still appear
        val labels = result.forYou.map { it.label }
        assert(labels.isNotEmpty()) { "forYou should not be empty" }
        assert(labels.contains("Jazz Reunion")) { "Jazz Reunion must appear in forYou for a jazz fan" }
    }

    @Test
    fun `should return empty forYou when user has no interests and no visit history`() {
        seedCatalog()
        userRepository.save(
            User(fullName = "New User", phone = "+70000000002", interests = emptyList(), id = userId())
        )

        val result = useCase.get(userId = userId(), city = "Ekaterinburg", page = 0, size = 10)

        // forYou is empty when there are no visits and no matching interests — by design
        assert(result.forYou.isEmpty()) { "forYou should be empty for a user with no history or interests" }
        // but byCategory must still be populated
        assert(result.byCategory.isNotEmpty()) { "byCategory must contain events" }
    }

    @Test
    fun `should exclude started and manually closed events from discovery`() {
        categoryRepository.save(Category(code = "rock", label = "Rock", id = rockCategoryId()))
        venueRepository.save(
            demoVenue(
                organizationId = orgOneId(),
                venueSpaceId = UUID.fromString("123e4567-e89b-12d3-a456-426614176208")
            )
        )
        eventRepository.save(
            demoEvent(
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614176216"),
                label = "Available Tomorrow",
                venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614176200"),
                categoryId = rockCategoryId(),
                time = LocalDate.now(ZoneOffset.UTC).plusDays(1).atTime(18, 0).toInstant(ZoneOffset.UTC),
                organizationId = orgOneId()
            )
        )
        eventRepository.save(
            demoEvent(
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614176217"),
                label = "Closed Tomorrow",
                venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614176200"),
                categoryId = rockCategoryId(),
                time = LocalDate.now(ZoneOffset.UTC).plusDays(1).atTime(19, 0).toInstant(ZoneOffset.UTC),
                organizationId = orgOneId()
            ).closeSales(Instant.parse("2026-03-22T12:00:00Z"))
        )
        eventRepository.save(
            demoEvent(
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614176218"),
                label = "Started Earlier Today",
                venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614176200"),
                categoryId = rockCategoryId(),
                time = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC),
                organizationId = orgOneId()
            )
        )

        val result = useCase.get(userId = userId(), city = "Ekaterinburg", page = 0, size = 10)

        assertEquals(listOf("Available Tomorrow"), result.tomorrow.map { it.label })
    }

    private fun seedCatalog() {
        categoryRepository.save(Category(code = "rock", label = "Rock", id = rockCategoryId()))
        categoryRepository.save(Category(code = "jazz", label = "Jazz", id = jazzCategoryId()))

        venueRepository.save(
            demoVenue(
                organizationId = orgOneId(),
                venueSpaceId = UUID.fromString("123e4567-e89b-12d3-a456-426614176208")
            )
        )
        venueRepository.save(
            demoVenue(
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614176202"),
                city = "Perm",
                organizationId = orgTwoId(),
                venueSpaceId = UUID.fromString("123e4567-e89b-12d3-a456-426614176203")
            )
        )

        eventRepository.save(
            demoEvent(
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614176210"),
                label = "Past Rock Visit",
                venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614176200"),
                categoryId = rockCategoryId(),
                time = LocalDate.now(ZoneOffset.UTC).minusDays(2).atTime(12, 0).toInstant(ZoneOffset.UTC),
                organizationId = orgOneId()
            )
        )
        eventRepository.save(
            demoEvent(
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614176211"),
                label = "Past Jazz Visit",
                venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614176200"),
                categoryId = jazzCategoryId(),
                time = LocalDate.now(ZoneOffset.UTC).minusDays(5).atTime(12, 0).toInstant(ZoneOffset.UTC),
                organizationId = orgOneId()
            )
        )
        eventRepository.save(
            demoEvent(
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614176212"),
                label = "Rock Tomorrow",
                venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614176200"),
                categoryId = rockCategoryId(),
                time = LocalDate.now(ZoneOffset.UTC).plusDays(1).atTime(18, 0).toInstant(ZoneOffset.UTC),
                organizationId = orgOneId()
            )
        )
        eventRepository.save(
            demoEvent(
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614176213"),
                label = "Indie Day After Tomorrow",
                venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614176200"),
                categoryId = rockCategoryId(),
                time = LocalDate.now(ZoneOffset.UTC).plusDays(2).atTime(18, 0).toInstant(ZoneOffset.UTC),
                organizationId = orgTwoId()
            )
        )
        eventRepository.save(
            demoEvent(
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614176214"),
                label = "Jazz Reunion",
                venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614176200"),
                categoryId = jazzCategoryId(),
                time = LocalDate.now(ZoneOffset.UTC).plusDays(3).atTime(18, 0).toInstant(ZoneOffset.UTC),
                organizationId = orgOneId()
            )
        )
        eventRepository.save(
            demoEvent(
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614176215"),
                label = "Perm Rock Show",
                venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614176202"),
                categoryId = rockCategoryId(),
                time = LocalDate.now(ZoneOffset.UTC).plusDays(1).atTime(20, 0).toInstant(ZoneOffset.UTC),
                organizationId = orgTwoId()
            )
        )
    }

    private fun demoVenue(id: UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614176200"), city: String = "Ekaterinburg", organizationId: UUID, venueSpaceId: UUID): Venue =
        Venue(
            label = "Demo Hall",
            city = City(label = city, subject = Subject(label = "Region")),
            organizationId = organizationId,
            id = id,
            spaces = listOf(VenueSpace(label = "Main Hall", id = venueSpaceId))
        )

    private fun demoEvent(
        id: UUID,
        label: String,
        venueId: UUID,
        categoryId: UUID,
        time: Instant,
        organizationId: UUID
    ): Event = Event(
        label = label,
        description = "Discovery test event",
        venueId = venueId,
        categoryId = categoryId,
        time = time,
        venueSpaceId = null,
        id = id,
        organizationId = organizationId
    )

    private fun userId(): UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614176201")
    private fun rockCategoryId(): UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614176204")
    private fun jazzCategoryId(): UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614176205")
    private fun orgOneId(): UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614176206")
    private fun orgTwoId(): UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614176207")
}

package com.karrad.bilets.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.VenueSpace
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.repository.CategoryRepository
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.domain.repository.VenueRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.util.UUID

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EventControllerIntegrationTests {

    lateinit var mockMvc: MockMvc

    @Autowired lateinit var authTokenRepository: AuthTokenRepository
    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var categoryRepository: CategoryRepository

    @Autowired
    lateinit var venueRepository: VenueRepository

    @Autowired
    lateinit var organizationRepository: OrganizationRepository

    @Autowired
    lateinit var organizationMemberRepository: OrganizationMemberRepository

    @Autowired
    lateinit var eventRepository: EventRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Test
    fun `should create event over http`() {
        val venue = demoVenue()
        val category = demoCategory("theatre", "Theatre", UUID.fromString("123e4567-e89b-12d3-a456-426614174440"))
        seedOrganizationAccess()
        categoryRepository.save(category)
        venueRepository.save(venue)

        mockMvc.perform(
            post("/api/v1/events")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(demoCreatorUserId())}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "label" to "Hamlet",
                            "description" to "Evening show",
                            "venueId" to venue.id,
                            "categoryId" to category.id,
                            "time" to "2026-04-10T18:00:00Z",
                            "venueSpaceId" to venue.spaces.first().id
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.label").value("Hamlet"))
            .andExpect(jsonPath("$.organizationId").value(venue.organizationId.toString()))
            .andExpect(jsonPath("$.venueId").value(venue.id.toString()))
            .andExpect(jsonPath("$.venueSpaceId").value(venue.spaces.first().id.toString()))
    }

    @Test
    fun `should create general admission event over http`() {
        val venue = demoVenue()
        val category = demoCategory("cinema", "Cinema", UUID.fromString("123e4567-e89b-12d3-a456-426614174441"))
        seedOrganizationAccess()
        categoryRepository.save(category)
        venueRepository.save(venue)

        mockMvc.perform(
            post("/api/v1/events")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(demoCreatorUserId())}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "label" to "Open Festival",
                            "description" to "Standing event",
                            "venueId" to venue.id,
                            "categoryId" to category.id,
                            "time" to "2026-04-10T18:00:00Z",
                            "venueSpaceId" to null
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.label").value("Open Festival"))
            .andExpect(jsonPath("$.venueSpaceId").doesNotExist())
    }

    @Test
    fun `should reject event creation when venue is missing`() {
        val category = demoCategory("theatre", "Theatre", UUID.fromString("123e4567-e89b-12d3-a456-426614174442"))
        seedOrganizationAccess()
        categoryRepository.save(category)

        mockMvc.perform(
            post("/api/v1/events")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(demoCreatorUserId())}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "label" to "Hamlet",
                            "description" to "Evening show",
                            "venueId" to "123e4567-e89b-12d3-a456-426614174421",
                            "categoryId" to category.id,
                            "time" to "2026-04-10T18:00:00Z"
                        )
                    )
                )
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should reject event creation when venue space does not belong to venue`() {
        val venue = demoVenue()
        val category = demoCategory("theatre", "Theatre", UUID.fromString("123e4567-e89b-12d3-a456-426614174443"))
        seedOrganizationAccess()
        categoryRepository.save(category)
        venueRepository.save(venue)

        mockMvc.perform(
            post("/api/v1/events")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(demoCreatorUserId())}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "label" to "Hamlet",
                            "description" to "Evening show",
                            "venueId" to venue.id,
                            "categoryId" to category.id,
                            "time" to "2026-04-10T18:00:00Z",
                            "venueSpaceId" to "123e4567-e89b-12d3-a456-426614174422"
                        )
                    )
                )
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should reject event creation when category is missing`() {
        val venue = demoVenue()
        seedOrganizationAccess()
        venueRepository.save(venue)

        mockMvc.perform(
            post("/api/v1/events")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(demoCreatorUserId())}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "label" to "Hamlet",
                            "description" to "Evening show",
                            "venueId" to venue.id,
                            "categoryId" to "123e4567-e89b-12d3-a456-426614174444",
                            "time" to "2026-04-10T18:00:00Z",
                            "venueSpaceId" to venue.spaces.first().id
                        )
                    )
                )
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should reject event creation when creator is not organization member`() {
        val venue = demoVenue()
        val category = demoCategory("theatre", "Theatre", UUID.fromString("123e4567-e89b-12d3-a456-426614174445"))
        organizationRepository.save(demoOrganization())
        categoryRepository.save(category)
        venueRepository.save(venue)

        mockMvc.perform(
            post("/api/v1/events")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(demoCreatorUserId())}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "label" to "Hamlet",
                            "description" to "Evening show",
                            "venueId" to venue.id,
                            "categoryId" to category.id,
                            "time" to "2026-04-10T18:00:00Z",
                            "venueSpaceId" to venue.spaces.first().id
                        )
                    )
                )
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should close event sales over http`() {
        val venue = demoVenue()
        val category = demoCategory("theatre", "Theatre", UUID.fromString("123e4567-e89b-12d3-a456-426614174446"))
        seedOrganizationAccess()
        categoryRepository.save(category)
        venueRepository.save(venue)

        val createResponse = mockMvc.perform(
            post("/api/v1/events")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(demoCreatorUserId())}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "label" to "Hamlet",
                            "description" to "Evening show",
                            "venueId" to venue.id,
                            "categoryId" to category.id,
                            "time" to "2026-04-10T18:00:00Z",
                            "venueSpaceId" to venue.spaces.first().id
                        )
                    )
                )
        ).andReturn()

        val eventId = objectMapper.readTree(createResponse.response.contentAsString).get("id").asText()

        mockMvc.perform(
            post("/api/v1/events/$eventId/close-sales")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(demoCreatorUserId())}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.salesClosedAt").isNotEmpty)
    }

    @Test
    fun `should create event with hasSeatMap true`() {
        val venue = demoVenue()
        val category = demoCategory("theatre", "Theatre", UUID.fromString("123e4567-e89b-12d3-a456-426614174447"))
        seedOrganizationAccess()
        categoryRepository.save(category)
        venueRepository.save(venue)

        mockMvc.perform(
            post("/api/v1/events")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(demoCreatorUserId())}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "label" to "Swan Lake",
                            "description" to "Ballet",
                            "venueId" to venue.id,
                            "categoryId" to category.id,
                            "time" to "2026-05-01T18:00:00Z",
                            "venueSpaceId" to venue.spaces.first().id,
                            "hasSeatMap" to true
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.hasSeatMap").value(true))
    }

    @Test
    fun `hasSeatMap defaults to false when not provided`() {
        val venue = demoVenue()
        val category = demoCategory("cinema", "Cinema", UUID.fromString("123e4567-e89b-12d3-a456-426614174448"))
        seedOrganizationAccess()
        categoryRepository.save(category)
        venueRepository.save(venue)

        mockMvc.perform(
            post("/api/v1/events")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(demoCreatorUserId())}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "label" to "Movie Night",
                            "description" to "Film screening",
                            "venueId" to venue.id,
                            "categoryId" to category.id,
                            "time" to "2026-05-02T20:00:00Z"
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.hasSeatMap").value(false))
    }

    @Test
    fun `should reject close-sales with 403 when caller is not organizer`() {
        val venue = demoVenue()
        val category = demoCategory("theatre", "Theatre", UUID.fromString("123e4567-e89b-12d3-a456-426614174449"))
        seedOrganizationAccess()
        categoryRepository.save(category)
        venueRepository.save(venue)

        val createResponse = mockMvc.perform(
            post("/api/v1/events")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(demoCreatorUserId())}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "label" to "Hamlet",
                            "description" to "Evening show",
                            "venueId" to venue.id,
                            "categoryId" to category.id,
                            "time" to "2026-04-10T18:00:00Z"
                        )
                    )
                )
        ).andReturn()

        val eventId = objectMapper.readTree(createResponse.response.contentAsString).get("id").asText()

        val outsider = User(
            email = "outsider@example.com",
            fullName = "Outsider",
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174450")
        )
        userRepository.save(outsider)

        mockMvc.perform(
            post("/api/v1/events/$eventId/close-sales")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(outsider.id)}")
        )
            .andExpect(status().isForbidden)
    }

    private fun demoVenue(): Venue {
        return Venue(
            label = "Demo Hall",
            city = City(label = "Ekaterinburg", subject = Subject(label = "Sverdlovsk Oblast")),
            organizationId = UUID.fromString("123e4567-e89b-12d3-a456-426614174432"),
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174430"),
            spaces = listOf(
                VenueSpace(
                    label = "Main Hall",
                    id = UUID.fromString("123e4567-e89b-12d3-a456-426614174431")
                )
            )
        )
    }

    private fun demoCategory(code: String, label: String, id: UUID): Category {
        return Category(code = code, label = label, id = id)
    }

    private fun seedOrganizationAccess() {
        organizationRepository.save(demoOrganization())
        userRepository.save(demoCreator())
        organizationMemberRepository.save(
            OrganizationMember(
                organizationId = demoOrganization().id,
                userId = demoCreatorUserId(),
                role = OrganizationMemberRole.OWNER
            )
        )
    }

    private fun demoOrganization(): Organization {
        return Organization(
            code = "demo-org",
            name = "Demo Org",
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174432")
        )
    }

    private fun demoCreatorUserId(): UUID =
        UUID.fromString("123e4567-e89b-12d3-a456-426614174446")

    private fun demoCreator(): User = User(
        email = "event-creator@example.com",
        fullName = "Event Creator",
        id = demoCreatorUserId()
    )
}

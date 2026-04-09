package com.karrad.bilets.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.repository.CategoryRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.FavoriteEventRepository
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.Instant
import java.util.UUID

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FavoriteEventControllerIntegrationTests {

    lateinit var mockMvc: MockMvc

    @Autowired lateinit var webApplicationContext: WebApplicationContext
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var eventRepository: EventRepository
    @Autowired lateinit var categoryRepository: CategoryRepository
    @Autowired lateinit var venueRepository: VenueRepository
    @Autowired lateinit var organizationRepository: OrganizationRepository
    @Autowired lateinit var organizationMemberRepository: OrganizationMemberRepository
    @Autowired lateinit var favoriteEventRepository: FavoriteEventRepository

    private val userId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")
    private val eventId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001")
    private val orgId = UUID.fromString("cccccccc-0000-0000-0000-000000000001")
    private val venueId = UUID.fromString("dddddddd-0000-0000-0000-000000000001")
    private val categoryId = UUID.fromString("eeeeeeee-0000-0000-0000-000000000001")

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        seedData()
    }

    @Test
    fun `should add event to favorites`() {
        mockMvc.perform(
            post("/api/favorites")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("eventId" to eventId)))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.userId").value(userId.toString()))
            .andExpect(jsonPath("$.eventId").value(eventId.toString()))
    }

    @Test
    fun `should return 404 when event does not exist`() {
        val unknownEventId = UUID.fromString("ffffffff-0000-0000-0000-000000000001")
        mockMvc.perform(
            post("/api/favorites")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("eventId" to unknownEventId)))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should list favorite events`() {
        mockMvc.perform(
            post("/api/favorites")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("eventId" to eventId)))
        ).andExpect(status().isCreated)

        mockMvc.perform(
            get("/api/favorites")
                .header("X-User-Id", userId.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(eventId.toString()))
    }

    @Test
    fun `should remove event from favorites`() {
        mockMvc.perform(
            post("/api/favorites")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("eventId" to eventId)))
        ).andExpect(status().isCreated)

        mockMvc.perform(
            delete("/api/favorites/$eventId")
                .header("X-User-Id", userId.toString())
        ).andExpect(status().isNoContent)

        mockMvc.perform(
            get("/api/favorites")
                .header("X-User-Id", userId.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `should return empty list when no favorites`() {
        mockMvc.perform(
            get("/api/favorites")
                .header("X-User-Id", userId.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `duplicate add is idempotent`() {
        repeat(2) {
            mockMvc.perform(
                post("/api/favorites")
                    .header("X-User-Id", userId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mapOf("eventId" to eventId)))
            ).andExpect(status().isCreated)
        }

        mockMvc.perform(
            get("/api/favorites")
                .header("X-User-Id", userId.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    private fun seedData() {
        val org = Organization(code = "fav-org", name = "Fav Org", id = orgId)
        val venue = Venue(
            label = "Hall",
            city = City(label = "Moscow", subject = Subject(label = "Moscow Oblast")),
            organizationId = orgId,
            id = venueId
        )
        val category = Category(code = "music", label = "Music", id = categoryId)
        val user = User(fullName = "Viewer", email = "viewer@example.com", id = userId)
        val event = Event(
            label = "Big Concert",
            description = "Live music",
            venueId = venueId,
            categoryId = categoryId,
            time = Instant.parse("2027-01-01T18:00:00Z"),
            id = eventId,
            organizationId = orgId
        )
        organizationRepository.save(org)
        organizationMemberRepository.save(
            OrganizationMember(organizationId = orgId, userId = userId, role = OrganizationMemberRole.OWNER)
        )
        venueRepository.save(venue)
        categoryRepository.save(category)
        userRepository.save(user)
        eventRepository.save(event)
    }
}

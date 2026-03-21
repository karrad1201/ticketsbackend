package com.karrad.bilets.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.VenueSpace
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.repository.CategoryRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.VenueRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
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
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "label" to "Hamlet",
                            "description" to "Evening show",
                            "creatorUserId" to demoCreatorUserId(),
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
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "label" to "Open Festival",
                            "description" to "Standing event",
                            "creatorUserId" to demoCreatorUserId(),
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
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "label" to "Hamlet",
                            "description" to "Evening show",
                            "creatorUserId" to demoCreatorUserId(),
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
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "label" to "Hamlet",
                            "description" to "Evening show",
                            "creatorUserId" to demoCreatorUserId(),
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
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "label" to "Hamlet",
                            "description" to "Evening show",
                            "creatorUserId" to demoCreatorUserId(),
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
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "label" to "Hamlet",
                            "description" to "Evening show",
                            "creatorUserId" to demoCreatorUserId(),
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
}

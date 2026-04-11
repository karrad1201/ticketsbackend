package com.karrad.bilets.web

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
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.Instant
import java.util.UUID

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MyOrganizationControllerIntegrationTests {

    lateinit var mockMvc: MockMvc

    @Autowired lateinit var webApplicationContext: WebApplicationContext
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var organizationRepository: OrganizationRepository
    @Autowired lateinit var organizationMemberRepository: OrganizationMemberRepository
    @Autowired lateinit var eventRepository: EventRepository
    @Autowired lateinit var venueRepository: VenueRepository
    @Autowired lateinit var categoryRepository: CategoryRepository
    @Autowired lateinit var authTokenRepository: AuthTokenRepository

    private val userId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")
    private val otherUserId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002")
    private val orgId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001")
    private val venueId = UUID.fromString("cccccccc-0000-0000-0000-000000000001")
    private val categoryId = UUID.fromString("dddddddd-0000-0000-0000-000000000001")

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        userRepository.save(User(fullName = "Owner", email = "owner@example.com", id = userId))
        userRepository.save(User(fullName = "No Org User", email = "noorg@example.com", id = otherUserId))
    }

    @Test
    fun `should return empty list when user has no organization membership`() {
        mockMvc.perform(
            get("/api/my/organization/events")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(otherUserId)}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `should return upcoming events for user organization`() {
        seedOrganizationWithEvent(time = Instant.parse("2027-06-01T18:00:00Z"))

        mockMvc.perform(
            get("/api/my/organization/events")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(userId)}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].label").value("My Event"))
    }

    @Test
    fun `should return empty list when organization has no upcoming events`() {
        organizationRepository.save(Organization(code = "my-org", name = "My Org", id = orgId))
        organizationMemberRepository.save(
            OrganizationMember(organizationId = orgId, userId = userId, role = OrganizationMemberRole.OWNER)
        )

        mockMvc.perform(
            get("/api/my/organization/events")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(userId)}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    private fun seedOrganizationWithEvent(time: Instant) {
        organizationRepository.save(Organization(code = "my-org", name = "My Org", id = orgId))
        organizationMemberRepository.save(
            OrganizationMember(organizationId = orgId, userId = userId, role = OrganizationMemberRole.OWNER)
        )
        venueRepository.save(
            Venue(
                label = "Hall",
                city = City(label = "Moscow", subject = Subject(label = "Moscow Oblast")),
                organizationId = orgId,
                id = venueId
            )
        )
        categoryRepository.save(Category(code = "music", label = "Music", id = categoryId))
        eventRepository.save(
            Event(
                label = "My Event",
                description = "Org event",
                venueId = venueId,
                categoryId = categoryId,
                time = time,
                organizationId = orgId,
                id = UUID.fromString("eeeeeeee-0000-0000-0000-000000000001")
            )
        )
    }
}

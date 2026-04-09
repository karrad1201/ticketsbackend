package com.karrad.bilets.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.VenueAccessGrant
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.domain.repository.VenueAccessGrantRepository
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
import java.time.Instant
import java.util.UUID

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class VenueAccessGrantControllerIntegrationTests {

    lateinit var mockMvc: MockMvc

    @Autowired lateinit var webApplicationContext: WebApplicationContext
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var venueRepository: VenueRepository
    @Autowired lateinit var organizationRepository: OrganizationRepository
    @Autowired lateinit var organizationMemberRepository: OrganizationMemberRepository
    @Autowired lateinit var venueAccessGrantRepository: VenueAccessGrantRepository

    // Venue owner
    private val ownerUserId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")
    private val ownerOrgId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001")
    private val venueId = UUID.fromString("cccccccc-0000-0000-0000-000000000001")

    // Requesting org
    private val requesterUserId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002")
    private val requesterOrgId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002")

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        seedData()
    }

    @Test
    fun `should request venue access`() {
        mockMvc.perform(
            post("/api/venues/$venueId/access-requests")
                .header("X-User-Id", requesterUserId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("requestingOrgId" to requesterOrgId)))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.venueId").value(venueId.toString()))
            .andExpect(jsonPath("$.requestingOrgId").value(requesterOrgId.toString()))
            .andExpect(jsonPath("$.status").value("PENDING"))
    }

    @Test
    fun `should list venue access requests`() {
        venueAccessGrantRepository.save(
            VenueAccessGrant(venueId = venueId, requestingOrgId = requesterOrgId, createdAt = Instant.now())
        )

        mockMvc.perform(
            get("/api/venues/$venueId/access-requests")
                .header("X-User-Id", ownerUserId.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].requestingOrgId").value(requesterOrgId.toString()))
    }

    @Test
    fun `should approve venue access request`() {
        val grant = venueAccessGrantRepository.save(
            VenueAccessGrant(venueId = venueId, requestingOrgId = requesterOrgId, createdAt = Instant.now())
        )

        mockMvc.perform(
            post("/api/venues/$venueId/access-requests/${grant.id}/approve")
                .header("X-User-Id", ownerUserId.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.decidedBy").value(ownerUserId.toString()))
    }

    @Test
    fun `should reject venue access request`() {
        val grant = venueAccessGrantRepository.save(
            VenueAccessGrant(venueId = venueId, requestingOrgId = requesterOrgId, createdAt = Instant.now())
        )

        mockMvc.perform(
            post("/api/venues/$venueId/access-requests/${grant.id}/reject")
                .header("X-User-Id", ownerUserId.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("REJECTED"))
    }

    @Test
    fun `should return 400 when requesting access to own venue`() {
        mockMvc.perform(
            post("/api/venues/$venueId/access-requests")
                .header("X-User-Id", ownerUserId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("requestingOrgId" to ownerOrgId)))
        ).andExpect(status().isBadRequest)
    }

    private fun seedData() {
        userRepository.save(User(fullName = "Owner", email = "owner@example.com", id = ownerUserId))
        userRepository.save(User(fullName = "Requester", email = "requester@example.com", id = requesterUserId))
        organizationRepository.save(Organization(code = "owner-org", name = "Owner Org", id = ownerOrgId))
        organizationRepository.save(Organization(code = "requester-org", name = "Requester Org", id = requesterOrgId))
        organizationMemberRepository.save(
            OrganizationMember(organizationId = ownerOrgId, userId = ownerUserId, role = OrganizationMemberRole.OWNER)
        )
        organizationMemberRepository.save(
            OrganizationMember(organizationId = requesterOrgId, userId = requesterUserId, role = OrganizationMemberRole.OWNER)
        )
        venueRepository.save(
            Venue(
                label = "Concert Hall",
                city = City(label = "Moscow", subject = Subject(label = "Moscow Oblast")),
                organizationId = ownerOrgId,
                id = venueId
            )
        )
    }
}

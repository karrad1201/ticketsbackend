package com.karrad.bilets.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.domain.repository.VenueRepository
import com.karrad.bilets.support.MockMvcSecurityHelper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.util.UUID

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LeaveOrganizationAndAddByPhoneIntegrationTests {

    lateinit var mockMvc: MockMvc

    @Autowired lateinit var webApplicationContext: WebApplicationContext
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var organizationRepository: OrganizationRepository
    @Autowired lateinit var organizationMemberRepository: OrganizationMemberRepository
    @Autowired lateinit var venueRepository: VenueRepository
    @Autowired lateinit var authTokenRepository: AuthTokenRepository

    private val orgId = UUID.fromString("dddddddd-0000-0000-0000-000000000001")
    private val owner1Id = UUID.fromString("dddddddd-0000-0000-0000-000000000002")
    private val owner2Id = UUID.fromString("dddddddd-0000-0000-0000-000000000003")
    private val managerId = UUID.fromString("dddddddd-0000-0000-0000-000000000004")
    private val venueId = UUID.fromString("dddddddd-0000-0000-0000-000000000005")

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .let { MockMvcSecurityHelper.withSpringSecurity(it) }
            .build()

        organizationRepository.save(Organization(code = "test-org", name = "Test Org", id = orgId))
        userRepository.save(User(email = "owner1@test.com", fullName = "Owner 1", id = owner1Id))
        userRepository.save(User(email = "owner2@test.com", fullName = "Owner 2", id = owner2Id))
        userRepository.save(User(email = "manager@test.com", fullName = "Manager", id = managerId))

        organizationMemberRepository.save(
            OrganizationMember(organizationId = orgId, userId = owner1Id, role = OrganizationMemberRole.OWNER)
        )
        organizationMemberRepository.save(
            OrganizationMember(organizationId = orgId, userId = owner2Id, role = OrganizationMemberRole.OWNER)
        )
        organizationMemberRepository.save(
            OrganizationMember(organizationId = orgId, userId = managerId, role = OrganizationMemberRole.MANAGER)
        )

        venueRepository.save(
            Venue(
                label = "Test Venue",
                city = City(label = "Москва", subject = Subject(label = "МО")),
                organizationId = orgId,
                id = venueId
            )
        )
    }

    // ─── Leave organization ───────────────────────────────────────────────────

    @Test
    fun `manager can leave organization`() {
        mockMvc.perform(
            delete("/api/v1/my/organization/membership")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(managerId)}")
        )
            .andExpect(status().isNoContent)

        assert(organizationMemberRepository.findByUserId(managerId).isEmpty())
    }

    @Test
    fun `owner can leave when another owner exists`() {
        mockMvc.perform(
            delete("/api/v1/my/organization/membership")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(owner1Id)}")
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `sole owner cannot leave organization`() {
        // Remove second owner first
        val owner2Membership = organizationMemberRepository.findByUserId(owner2Id).first()
        organizationMemberRepository.deleteById(owner2Membership.id)

        mockMvc.perform(
            delete("/api/v1/my/organization/membership")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(owner1Id)}")
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `unauthenticated cannot leave organization`() {
        mockMvc.perform(delete("/api/v1/my/organization/membership"))
            .andExpect(status().isUnauthorized)
    }

    // ─── Add member by phone ──────────────────────────────────────────────────

    @Test
    fun `owner can add existing user by phone`() {
        val existingUser = userRepository.save(User(phone = "+79001234567", fullName = "Existing User"))

        mockMvc.perform(
            post("/api/v1/my/organization/members/by-phone")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(owner1Id)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf("phone" to "+79001234567", "role" to "MANAGER", "venueId" to venueId.toString())
                    )
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.accountCreated").value(false))
            .andExpect(jsonPath("$.member.role").value("MANAGER"))

        assert(organizationMemberRepository.findByUserId(existingUser.id).isNotEmpty())
    }

    @Test
    fun `owner adds new user by phone — stub account created`() {
        mockMvc.perform(
            post("/api/v1/my/organization/members/by-phone")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(owner1Id)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf("phone" to "+79009998877", "role" to "MANAGER", "venueId" to venueId.toString())
                    )
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.accountCreated").value(true))

        val stubUser = userRepository.findByPhone("+79009998877")
        assert(stubUser != null)
        assert(organizationMemberRepository.findByUserId(requireNotNull(stubUser).id).isNotEmpty())
    }

    @Test
    fun `cannot add user already in another organization`() {
        val otherOrgId = UUID.fromString("dddddddd-0000-0000-0000-000000000099")
        organizationRepository.save(Organization(code = "other-org", name = "Other Org", id = otherOrgId))
        val alreadyMemberUser = userRepository.save(User(phone = "+79111111111", fullName = "Already Member"))
        organizationMemberRepository.save(
            OrganizationMember(organizationId = otherOrgId, userId = alreadyMemberUser.id, role = OrganizationMemberRole.MANAGER)
        )

        mockMvc.perform(
            post("/api/v1/my/organization/members/by-phone")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(owner1Id)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf("phone" to "+79111111111", "role" to "MANAGER", "venueId" to venueId.toString())
                    )
                )
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `invalid phone format returns 400`() {
        mockMvc.perform(
            post("/api/v1/my/organization/members/by-phone")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(owner1Id)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf("phone" to "89001234567", "role" to "MANAGER")
                    )
                )
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `staff member requires venueId`() {
        mockMvc.perform(
            post("/api/v1/my/organization/members/by-phone")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(owner1Id)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf("phone" to "+79009998800", "role" to "STAFF")
                    )
                )
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `owner can add staff with venueId by phone`() {
        mockMvc.perform(
            post("/api/v1/my/organization/members/by-phone")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(owner1Id)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf("phone" to "+79009997766", "role" to "STAFF", "venueId" to venueId.toString())
                    )
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.member.role").value("STAFF"))
            .andExpect(jsonPath("$.member.venueId").value(venueId.toString()))
    }
}

package com.karrad.bilets.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.domain.repository.VenueApplicationRepository
import com.karrad.bilets.support.MockMvcSecurityHelper
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
class VenueApplicationControllerIntegrationTests {

    lateinit var mockMvc: MockMvc

    @Autowired lateinit var webApplicationContext: WebApplicationContext
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var organizationRepository: OrganizationRepository
    @Autowired lateinit var organizationMemberRepository: OrganizationMemberRepository
    @Autowired lateinit var authTokenRepository: AuthTokenRepository
    @Autowired lateinit var venueApplicationRepository: VenueApplicationRepository

    private val orgId = UUID.fromString("cccccccc-0000-0000-0000-000000000001")
    private val ownerId = UUID.fromString("cccccccc-0000-0000-0000-000000000002")
    private val adminId = UUID.fromString("cccccccc-0000-0000-0000-000000000003")
    private val managerId = UUID.fromString("cccccccc-0000-0000-0000-000000000004")

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .let { MockMvcSecurityHelper.withSpringSecurity(it) }
            .build()

        organizationRepository.save(Organization(code = "test-org", name = "Test Org", id = orgId))
        userRepository.save(User(email = "owner@test.com", fullName = "Owner", id = ownerId))
        userRepository.save(User(email = "admin@test.com", fullName = "Admin", role = UserRole.ADMIN, id = adminId))
        userRepository.save(User(email = "manager@test.com", fullName = "Manager", id = managerId))

        organizationMemberRepository.save(
            OrganizationMember(organizationId = orgId, userId = ownerId, role = OrganizationMemberRole.OWNER)
        )
        organizationMemberRepository.save(
            OrganizationMember(organizationId = orgId, userId = managerId, role = OrganizationMemberRole.MANAGER)
        )
    }

    @Test
    fun `owner can submit venue application`() {
        mockMvc.perform(
            post("/api/v1/my/organization/venue-applications")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(ownerId)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest()))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.name").value("Арена Тест"))
    }

    @Test
    fun `manager cannot submit venue application`() {
        mockMvc.perform(
            post("/api/v1/my/organization/venue-applications")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(managerId)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest()))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `owner can list own applications`() {
        mockMvc.perform(
            post("/api/v1/my/organization/venue-applications")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(ownerId)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest()))
        ).andExpect(status().isCreated)

        mockMvc.perform(
            get("/api/v1/my/organization/venue-applications")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(ownerId)}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `admin can approve application and venue is created`() {
        mockMvc.perform(
            post("/api/v1/my/organization/venue-applications")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(ownerId)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest()))
        ).andExpect(status().isCreated)

        val application = venueApplicationRepository.findByOrganizationId(orgId).first()

        mockMvc.perform(
            post("/api/v1/venue-applications/${application.id}/approve")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(adminId)}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.venueId").isNotEmpty)
    }

    @Test
    fun `admin can reject application`() {
        mockMvc.perform(
            post("/api/v1/my/organization/venue-applications")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(ownerId)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest()))
        ).andExpect(status().isCreated)

        val application = venueApplicationRepository.findByOrganizationId(orgId).first()

        mockMvc.perform(
            post("/api/v1/venue-applications/${application.id}/reject")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(adminId)}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("REJECTED"))
    }

    @Test
    fun `non-admin cannot approve application`() {
        mockMvc.perform(
            post("/api/v1/my/organization/venue-applications")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(ownerId)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest()))
        ).andExpect(status().isCreated)

        val application = venueApplicationRepository.findByOrganizationId(orgId).first()

        mockMvc.perform(
            post("/api/v1/venue-applications/${application.id}/approve")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(ownerId)}")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `admin can list all applications filtered by status`() {
        mockMvc.perform(
            post("/api/v1/my/organization/venue-applications")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(ownerId)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest()))
        ).andExpect(status().isCreated)

        mockMvc.perform(
            get("/api/v1/venue-applications?status=PENDING")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(adminId)}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    private fun validRequest() = mapOf(
        "name" to "Арена Тест",
        "cityLabel" to "Москва",
        "subjectLabel" to "Московская область",
        "address" to "ул. Тестовая, 1",
        "description" to "Тестовое описание"
    )
}

package com.karrad.bilets.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.OrganizationApplicationRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.UserRepository
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
class OrganizationApplicationControllerIntegrationTests {

    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var organizationRepository: OrganizationRepository

    @Autowired
    lateinit var organizationMemberRepository: OrganizationMemberRepository

    @Autowired
    lateinit var organizationApplicationRepository: OrganizationApplicationRepository

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Test
    fun `should submit and approve organization application over http`() {
        val applicant = demoUser()
        val admin = demoAdmin()
        userRepository.save(applicant)
        userRepository.save(admin)

        mockMvc.perform(
            post("/api/organization-applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "applicantUserId" to applicant.id,
                            "organizationCode" to "ural-live",
                            "organizationName" to "Ural Live Events"
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("PENDING"))

        val application = organizationApplicationRepository.findPendingByOrganizationCode("ural-live")!!

        mockMvc.perform(get("/api/organization-applications"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))

        mockMvc.perform(
            post("/api/organization-applications/${application.id}/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("adminUserId" to admin.id)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.organizationId").isNotEmpty)

        mockMvc.perform(get("/api/organization-applications/${application.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("APPROVED"))

        val organization = organizationRepository.findByCode("ural-live")
        assert(organization != null)
        val member = organizationMemberRepository.findByOrganizationIdAndUserId(requireNotNull(organization).id, applicant.id)
        assert(member != null)
        assert(requireNotNull(member).role == OrganizationMemberRole.OWNER)
    }

    @Test
    fun `should reject organization application when reviewer is not admin over http`() {
        val applicant = demoUser()
        userRepository.save(applicant)

        mockMvc.perform(
            post("/api/organization-applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "applicantUserId" to applicant.id,
                            "organizationCode" to "ural-live",
                            "organizationName" to "Ural Live Events"
                        )
                    )
                )
        )
            .andExpect(status().isCreated)

        val application = organizationApplicationRepository.findPendingByOrganizationCode("ural-live")!!

        mockMvc.perform(
            post("/api/organization-applications/${application.id}/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("adminUserId" to applicant.id)))
        )
            .andExpect(status().isBadRequest)
    }

    private fun demoUser(): User {
        return User(
            email = "user@example.com",
            fullName = "Regular User",
            role = UserRole.USER,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614175411")
        )
    }

    private fun demoAdmin(): User {
        return User(
            email = "admin@example.com",
            fullName = "Platform Admin",
            role = UserRole.ADMIN,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614175412")
        )
    }
}

package com.karrad.bilets.web

import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.UserRepository
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
import java.util.UUID

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrganizationMemberControllerIntegrationTests {

    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    lateinit var organizationMemberRepository: OrganizationMemberRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var authTokenRepository: AuthTokenRepository

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Test
    fun `should read organization members over http`() {
        val admin = User(
            email = "admin@example.com",
            fullName = "Platform Admin",
            role = UserRole.ADMIN,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614175420")
        )
        userRepository.save(admin)

        val organization = Organization(
            code = "ural-live",
            name = "Ural Live Events",
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614175421")
        )
        val user = User(
            email = "user@example.com",
            fullName = "Regular User",
            role = UserRole.USER,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614175422")
        )
        val member = OrganizationMember(
            organizationId = organization.id,
            userId = user.id,
            role = OrganizationMemberRole.OWNER,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614175423")
        )
        organizationMemberRepository.save(member)

        val adminBearer = "Bearer ${authTokenRepository.bearerFor(admin.id)}"

        mockMvc.perform(get("/api/organization-members").header("Authorization", adminBearer))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))

        mockMvc.perform(get("/api/organization-members/${member.id}").header("Authorization", adminBearer))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.role").value("OWNER"))

        mockMvc.perform(
            get("/api/organization-members")
                .header("Authorization", adminBearer)
                .param("organizationId", organization.id.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))

        mockMvc.perform(
            get("/api/organization-members")
                .header("Authorization", adminBearer)
                .param("userId", user.id.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `should reject organization members listing without admin token`() {
        mockMvc.perform(get("/api/organization-members"))
            .andExpect(status().isUnauthorized)
    }
}

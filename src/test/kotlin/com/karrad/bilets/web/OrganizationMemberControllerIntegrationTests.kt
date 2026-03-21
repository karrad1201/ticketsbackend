package com.karrad.bilets.web

import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
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

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Test
    fun `should read organization members over http`() {
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

        mockMvc.perform(get("/api/organization-members"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))

        mockMvc.perform(get("/api/organization-members/${member.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.role").value("OWNER"))

        mockMvc.perform(get("/api/organization-members").param("organizationId", organization.id.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))

        mockMvc.perform(get("/api/organization-members").param("userId", user.id.toString()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }
}

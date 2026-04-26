package com.karrad.bilets.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.AuthTokenRepository
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
import com.karrad.bilets.support.MockMvcSecurityHelper
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.util.UUID

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrganizationControllerIntegrationTests {

    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var organizationRepository: OrganizationRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var authTokenRepository: AuthTokenRepository

    @BeforeEach
    fun setUp() {
        val builder = MockMvcBuilders.webAppContextSetup(webApplicationContext)

        mockMvc = MockMvcSecurityHelper.withSpringSecurity(builder).build()
    }

    @Test
    fun `should create and read organizations over http`() {
        val admin = demoAdmin()
        userRepository.save(admin)

        mockMvc.perform(
            post("/api/v1/organizations")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(admin.id)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("code" to "ufa-jazz", "name" to "Ufa Jazz Collective")))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value("ufa-jazz"))

        val organization = organizationRepository.findByCode("ufa-jazz")!!

        mockMvc.perform(get("/api/v1/organizations"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))

        mockMvc.perform(get("/api/v1/organizations/${organization.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Ufa Jazz Collective"))
    }

    @Test
    fun `should reject duplicate organization code over http`() {
        val admin = demoAdmin()
        userRepository.save(admin)

        organizationRepository.save(
            Organization(
                code = "ufa-jazz",
                name = "Ufa Jazz Collective",
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614175301")
            )
        )

        mockMvc.perform(
            post("/api/v1/organizations")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(admin.id)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("code" to "ufa-jazz", "name" to "Another Label")))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should reject organization creation without authentication`() {
        mockMvc.perform(
            post("/api/v1/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("code" to "ufa-jazz", "name" to "Ufa Jazz Collective")))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should reject organization creation for non-admin user`() {
        val regularUser = demoUser()
        userRepository.save(regularUser)

        mockMvc.perform(
            post("/api/v1/organizations")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(regularUser.id)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("code" to "ufa-jazz", "name" to "Ufa Jazz Collective")))
        )
            .andExpect(status().isForbidden)
    }

    private fun demoAdmin(): User {
        return User(
            email = "admin@example.com",
            fullName = "Platform Admin",
            role = UserRole.ADMIN,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614175302")
        )
    }

    private fun demoUser(): User {
        return User(
            email = "user@example.com",
            fullName = "Regular User",
            role = UserRole.USER,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614175303")
        )
    }
}

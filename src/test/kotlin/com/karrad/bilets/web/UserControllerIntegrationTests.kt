package com.karrad.bilets.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.AuthTokenRepository
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
class UserControllerIntegrationTests {

    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var authTokenRepository: AuthTokenRepository

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Test
    fun `should create and read users over http`() {
        val admin = User(
            email = "admin@example.com",
            fullName = "Platform Admin",
            role = UserRole.ADMIN,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614175400")
        )
        userRepository.save(admin)

        mockMvc.perform(
            post("/api/v1/users")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(admin.id)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf("email" to "user@example.com", "fullName" to "Regular User", "role" to "USER")
                    )
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.email").value("user@example.com"))
            .andExpect(jsonPath("$.role").value("USER"))

        val user = userRepository.findByEmail("user@example.com")!!

        val adminBearer = "Bearer ${authTokenRepository.bearerFor(admin.id)}"

        mockMvc.perform(get("/api/v1/users").header("Authorization", adminBearer))
            .andExpect(status().isOk)

        mockMvc.perform(get("/api/v1/users/${user.id}").header("Authorization", adminBearer))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.fullName").value("Regular User"))
    }

    @Test
    fun `should reject user creation without admin token`() {
        mockMvc.perform(
            post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf("email" to "user@example.com", "fullName" to "Regular User", "role" to "USER")
                    )
                )
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should reject user creation when caller is not admin`() {
        val user = User(
            email = "user@example.com",
            fullName = "Regular User",
            role = UserRole.USER,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614175401")
        )
        userRepository.save(user)

        mockMvc.perform(
            post("/api/v1/users")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(user.id)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf("email" to "another@example.com", "fullName" to "Another User", "role" to "USER")
                    )
                )
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should reject duplicate user email over http`() {
        val admin = User(
            email = "admin@example.com",
            fullName = "Platform Admin",
            role = UserRole.ADMIN,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614175400")
        )
        userRepository.save(admin)
        userRepository.save(
            User(
                email = "user@example.com",
                fullName = "Regular User",
                role = UserRole.USER,
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614175401")
            )
        )

        mockMvc.perform(
            post("/api/v1/users")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(admin.id)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf("email" to "user@example.com", "fullName" to "Second User", "role" to "USER")
                    )
                )
        )
            .andExpect(status().isBadRequest)
    }
}

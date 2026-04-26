package com.karrad.bilets.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.CategoryRepository
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
class CategoryControllerIntegrationTests {

    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var categoryRepository: CategoryRepository

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
    fun `should create and read categories over http`() {
        val admin = User(
            email = "admin@example.com",
            fullName = "Platform Admin",
            role = UserRole.ADMIN,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614175210")
        )
        userRepository.save(admin)

        mockMvc.perform(
            post("/api/v1/categories")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(admin.id)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("code" to "theatre", "label" to "Theatre")))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value("theatre"))

        val category = categoryRepository.findByCode("theatre")!!

        mockMvc.perform(get("/api/v1/categories"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))

        mockMvc.perform(get("/api/v1/categories/${category.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.label").value("Theatre"))
    }

    @Test
    fun `should reject duplicate category code over http`() {
        val admin = User(
            email = "admin@example.com",
            fullName = "Platform Admin",
            role = UserRole.ADMIN,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614175211")
        )
        userRepository.save(admin)

        categoryRepository.save(Category(code = "theatre", label = "Theatre", id = UUID.fromString("123e4567-e89b-12d3-a456-426614175201")))

        mockMvc.perform(
            post("/api/v1/categories")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(admin.id)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("code" to "theatre", "label" to "Drama")))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should reject category creation without authentication`() {
        mockMvc.perform(
            post("/api/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("code" to "theatre", "label" to "Theatre")))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should reject category creation for non admin user`() {
        val user = User(
            email = "user@example.com",
            fullName = "Regular User",
            role = UserRole.USER,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614175212")
        )
        userRepository.save(user)

        mockMvc.perform(
            post("/api/v1/categories")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(user.id)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("code" to "theatre", "label" to "Theatre")))
        )
            .andExpect(status().isForbidden)
    }
}

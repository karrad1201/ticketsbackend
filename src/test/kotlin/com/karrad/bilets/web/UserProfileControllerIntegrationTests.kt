package com.karrad.bilets.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.util.UUID

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserProfileControllerIntegrationTests {

    lateinit var mockMvc: MockMvc

    @Autowired lateinit var webApplicationContext: WebApplicationContext
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var authTokenRepository: AuthTokenRepository

    private val userId = UUID.fromString("dddddddd-0000-0000-0000-000000000001")

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        userRepository.save(User(fullName = "Old Name", email = "profile@example.com", id = userId))
    }

    @Test
    fun `should update fullName via PATCH auth me`() {
        mockMvc.perform(
            patch("/auth/me")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(userId)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("fullName" to "New Name")))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.fullName").value("New Name"))
            .andExpect(jsonPath("$.id").value(userId.toString()))
    }

    @Test
    fun `should update interests via PATCH auth me`() {
        mockMvc.perform(
            patch("/auth/me")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(userId)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("interests" to listOf("music", "theatre"))))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.interests[0]").value("music"))
            .andExpect(jsonPath("$.interests[1]").value("theatre"))
    }

    @Test
    fun `should upload avatar and return updated avatarUrl`() {
        val file = MockMultipartFile(
            "file",
            "avatar.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            byteArrayOf(1, 2, 3, 4)
        )

        mockMvc.perform(
            multipart("/auth/me/avatar")
                .file(file)
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(userId)}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.avatarUrl").value(org.hamcrest.Matchers.containsString(userId.toString())))
            .andExpect(jsonPath("$.avatarUrl").value(org.hamcrest.Matchers.containsString("uploads/avatars")))
    }

    @Test
    fun `PATCH auth me without auth returns 401`() {
        mockMvc.perform(
            patch("/auth/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("fullName" to "Hacker")))
        )
            .andExpect(status().isUnauthorized)
    }
}

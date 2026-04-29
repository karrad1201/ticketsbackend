package com.karrad.bilets.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.karrad.bilets.domain.entity.SmsCode
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.RefreshTokenRepository
import com.karrad.bilets.domain.repository.SmsCodeRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.domain.security.OtpHasher
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
import java.time.Instant
import java.util.UUID

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthControllerIntegrationTests {

    lateinit var mockMvc: MockMvc

    @Autowired lateinit var webApplicationContext: WebApplicationContext
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var smsCodeRepository: SmsCodeRepository
    @Autowired lateinit var authTokenRepository: AuthTokenRepository
    @Autowired lateinit var refreshTokenRepository: RefreshTokenRepository

    private val phone = "+79991234567"
    private val code = "123456"

    @BeforeEach
    fun setUp() {
        val builder = MockMvcBuilders.webAppContextSetup(webApplicationContext)

        mockMvc = MockMvcSecurityHelper.withSpringSecurity(builder).build()
    }

    @Test
    fun `should send sms code`() {
        mockMvc.perform(
            post("/auth/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("phone" to phone)))
        ).andExpect(status().isNoContent)
    }

    @Test
    fun `should register new user and return token`() {
        seedSmsCode()

        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf(
                    "phone" to phone,
                    "code" to code,
                    "fullName" to "Test User"
                )))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.user.fullName").value("Test User"))
            .andExpect(jsonPath("$.user.phone").value(phone))
    }

    @Test
    fun `should login existing user and return token`() {
        userRepository.save(User(fullName = "Existing User", phone = phone, id = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")))
        seedSmsCode()

        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf(
                    "phone" to phone,
                    "code" to code
                )))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.user.fullName").value("Existing User"))
    }

    @Test
    fun `should return 400 when login with invalid code`() {
        seedSmsCode()

        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf(
                    "phone" to phone,
                    "code" to "000000"
                )))
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should return current user via me endpoint`() {
        val user = userRepository.save(User(
            fullName = "Me User",
            email = "me@example.com",
            id = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002")
        ))

        mockMvc.perform(
            get("/auth/me")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(user.id)}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(user.id.toString()))
            .andExpect(jsonPath("$.fullName").value("Me User"))
    }

    @Test
    fun `should refresh token pair`() {
        userRepository.save(User(fullName = "Refresh User", phone = phone, id = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000003")))
        seedSmsCode()

        val loginResponse = mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("phone" to phone, "code" to code)))
        ).andExpect(status().isOk).andReturn()

        val body = objectMapper.readTree(loginResponse.response.contentAsString)
        val refreshToken = body.get("refreshToken").asText()

        mockMvc.perform(
            post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("refreshToken" to refreshToken)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.refreshToken").isNotEmpty)
    }

    @Test
    fun `refresh with invalid token returns 401`() {
        mockMvc.perform(
            post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("refreshToken" to "invalid-token")))
        ).andExpect(status().isUnauthorized)
    }

    private fun seedSmsCode() {
        smsCodeRepository.save(
            SmsCode(
                phone = phone,
                code = OtpHasher.hash(phone, code),
                expiresAt = Instant.now().plusSeconds(300)
            )
        )
    }
}

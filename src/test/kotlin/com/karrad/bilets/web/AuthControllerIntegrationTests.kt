package com.karrad.bilets.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.karrad.bilets.domain.entity.SmsCode
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.SmsCodeRepository
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

    private val phone = "+79991234567"
    private val code = "123456"

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
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
            .andExpect(jsonPath("$.token").isNotEmpty)
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
            .andExpect(jsonPath("$.token").isNotEmpty)
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
                .header("X-User-Id", user.id.toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(user.id.toString()))
            .andExpect(jsonPath("$.fullName").value("Me User"))
    }

    private fun seedSmsCode() {
        smsCodeRepository.save(
            SmsCode(
                phone = phone,
                code = code,
                expiresAt = Instant.now().plusSeconds(300)
            )
        )
    }
}

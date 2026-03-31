package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.SmsCode
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.SmsCodeRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.support.MutableClock
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringJUnitConfig(ApplicationServicesTestConfig::class)
@Import(RegisterWithPhoneUseCase::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RegisterWithPhoneUseCaseTests {

    @Autowired lateinit var smsCodeRepository: SmsCodeRepository
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var authTokenRepository: AuthTokenRepository
    @Autowired lateinit var mutableClock: MutableClock
    @Autowired lateinit var useCase: RegisterWithPhoneUseCase

    private val phone = "+79001234567"

    private fun saveValidCode(code: String = "654321") {
        smsCodeRepository.save(
            SmsCode(phone = phone, code = code, expiresAt = mutableClock.instant().plusSeconds(300))
        )
    }

    @Test
    fun `should register new user`() {
        saveValidCode()

        val result = useCase.register(phone, "654321", "Новый Пользователь")

        assertEquals(phone, result.user.phone)
        assertEquals("Новый Пользователь", result.user.fullName)
        assertNotNull(userRepository.findByPhone(phone))
        assertNotNull(authTokenRepository.findByToken(result.token))
    }

    @Test
    fun `should fail when no code was sent`() {
        assertFailsWith<IllegalArgumentException> {
            useCase.register(phone, "654321", "User")
        }
    }

    @Test
    fun `should fail on wrong code`() {
        saveValidCode()

        val ex = assertFailsWith<IllegalArgumentException> {
            useCase.register(phone, "000000", "User")
        }
        assertTrue(ex.message!!.contains("Invalid code"))
    }

    @Test
    fun `should fail when phone already registered`() {
        saveValidCode()
        userRepository.save(User(fullName = "Existing", phone = phone))

        val ex = assertFailsWith<IllegalArgumentException> {
            useCase.register(phone, "654321", "New User")
        }
        assertTrue(ex.message!!.contains("already registered"))
    }

    @Test
    fun `should fail on expired code`() {
        smsCodeRepository.save(
            SmsCode(phone = phone, code = "654321", expiresAt = mutableClock.instant().minusSeconds(1))
        )

        val ex = assertFailsWith<IllegalArgumentException> {
            useCase.register(phone, "654321", "User")
        }
        assertTrue(ex.message!!.contains("expired"))
    }
}

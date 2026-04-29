package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.SmsCode
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.SmsCodeRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.domain.security.OtpHasher
import com.karrad.bilets.support.MutableClock
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringJUnitConfig(ApplicationServicesTestConfig::class)
@Import(LoginWithPhoneUseCase::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LoginWithPhoneUseCaseTests {

    @Autowired lateinit var smsCodeRepository: SmsCodeRepository
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var authTokenRepository: AuthTokenRepository
    @Autowired lateinit var mutableClock: MutableClock
    @Autowired lateinit var useCase: LoginWithPhoneUseCase

    private val phone = "+79001234567"

    private fun saveValidCode(rawCode: String = "123456") {
        smsCodeRepository.save(
            SmsCode(
                phone = phone,
                code = OtpHasher.hash(phone, rawCode),
                expiresAt = mutableClock.instant().plusSeconds(300)
            )
        )
    }

    @Test
    fun `should login existing user`() {
        saveValidCode("123456")
        userRepository.save(User(fullName = "Ivan", phone = phone))

        val result = useCase.login(phone, "123456")

        assertEquals(phone, result.user.phone)
        assertNotNull(authTokenRepository.findByToken(result.accessToken))
    }

    @Test
    fun `should invalidate old token on repeated login`() {
        userRepository.save(User(fullName = "Ivan", phone = phone))

        saveValidCode("111111")
        val first = useCase.login(phone, "111111")

        mutableClock.advanceByMinutes(1)
        saveValidCode("222222")
        val second = useCase.login(phone, "222222")

        assertNull(authTokenRepository.findByToken(first.accessToken), "Old token must be invalidated after re-login")
        assertNotNull(authTokenRepository.findByToken(second.accessToken), "New token must be active")
    }

    @Test
    fun `should fail when no code was sent`() {
        assertFailsWith<IllegalArgumentException> {
            useCase.login(phone, "123456")
        }
    }

    @Test
    fun `should fail on wrong code`() {
        saveValidCode("123456")
        userRepository.save(User(fullName = "Ivan", phone = phone))

        val ex = assertFailsWith<IllegalArgumentException> {
            useCase.login(phone, "000000")
        }
        assertTrue(ex.message!!.contains("Invalid code"))
    }

    @Test
    fun `should fail on expired code`() {
        smsCodeRepository.save(
            SmsCode(
                phone = phone,
                code = OtpHasher.hash(phone, "123456"),
                expiresAt = mutableClock.instant().minusSeconds(1)
            )
        )
        userRepository.save(User(fullName = "Ivan", phone = phone))

        val ex = assertFailsWith<IllegalArgumentException> {
            useCase.login(phone, "123456")
        }
        assertTrue(ex.message!!.contains("expired"))
    }

    @Test
    fun `should fail when user not found for phone`() {
        saveValidCode("123456")

        assertFailsWith<NoSuchElementException> {
            useCase.login(phone, "123456")
        }
    }

    @Test
    fun `brute force — code is single use, second attempt fails even with correct code`() {
        saveValidCode("123456")
        userRepository.save(User(fullName = "Ivan", phone = phone))

        useCase.login(phone, "123456")

        val ex = assertFailsWith<IllegalArgumentException> {
            useCase.login(phone, "123456")
        }
        assertTrue(ex.message!!.contains("used"))
    }

    @Test
    fun `brute force — multiple wrong codes do not consume the valid code`() {
        saveValidCode("123456")
        userRepository.save(User(fullName = "Ivan", phone = phone))

        repeat(3) {
            assertFailsWith<IllegalArgumentException> {
                useCase.login(phone, "000000")
            }
        }

        val result = useCase.login(phone, "123456")
        assertNotNull(authTokenRepository.findByToken(result.accessToken))
    }

    @Test
    fun `should fail on already used code`() {
        val code = smsCodeRepository.save(
            SmsCode(phone = phone, code = OtpHasher.hash(phone, "123456"), expiresAt = mutableClock.instant().plusSeconds(300))
        )
        smsCodeRepository.markUsed(code.id)
        userRepository.save(User(fullName = "Ivan", phone = phone))

        val ex = assertFailsWith<IllegalArgumentException> {
            useCase.login(phone, "123456")
        }
        assertTrue(ex.message!!.contains("used"))
    }
}

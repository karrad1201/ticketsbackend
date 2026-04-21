package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.SmsCode
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.security.OtpHasher
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryAuthTokenRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemorySmsCodeRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryUserRepository
import com.karrad.bilets.support.MutableClock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Конкурентные тесты для LoginWithPhoneUseCase.
 *
 * Issue: между findLatestByPhone() и tryMarkUsed() теоретически возможен race condition —
 * два потока могут одновременно пройти isValid() до того, как один из них пометит код.
 * InMemorySmsCodeRepository.tryMarkUsed() — @Synchronized, поэтому только один победит.
 * Тесты документируют обязательный контракт и защищают от регрессии.
 */
class LoginConcurrencyTests {

    private lateinit var clock: MutableClock
    private lateinit var smsCodeRepository: InMemorySmsCodeRepository
    private lateinit var userRepository: InMemoryUserRepository
    private lateinit var authTokenRepository: InMemoryAuthTokenRepository
    private lateinit var useCase: LoginWithPhoneUseCase

    private val phone = "+79001234567"
    private val rawCode = "123456"

    @BeforeEach
    fun setUp() {
        clock = MutableClock(Instant.parse("2026-01-01T00:00:00Z"))
        smsCodeRepository = InMemorySmsCodeRepository()
        userRepository = InMemoryUserRepository()
        authTokenRepository = InMemoryAuthTokenRepository()
        useCase = LoginWithPhoneUseCase(smsCodeRepository, userRepository, authTokenRepository, clock)
    }

    @Test
    fun `exactly one login should succeed when N threads use the same code simultaneously`() {
        userRepository.save(User(fullName = "Ivan Petrov", phone = phone))
        smsCodeRepository.save(
            SmsCode(
                phone = phone,
                code = OtpHasher.hash(phone, rawCode),
                expiresAt = clock.instant().plusSeconds(300)
            )
        )

        val threadCount = 20
        val startLatch = CountDownLatch(1)
        val successCount = AtomicInteger(0)
        val alreadyUsedCount = AtomicInteger(0)
        val unexpectedErrors = mutableListOf<Throwable>()
        val executor = Executors.newFixedThreadPool(threadCount)

        repeat(threadCount) {
            executor.submit {
                startLatch.await()
                try {
                    useCase.login(phone, rawCode)
                    successCount.incrementAndGet()
                } catch (e: IllegalArgumentException) {
                    // "Code already used" или "Invalid code" — ожидаемо для проигравших потоков
                    alreadyUsedCount.incrementAndGet()
                } catch (e: Exception) {
                    synchronized(unexpectedErrors) { unexpectedErrors.add(e) }
                }
            }
        }

        startLatch.countDown()
        executor.shutdown()
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "Threads must complete in time")

        assertTrue(unexpectedErrors.isEmpty(), "Unexpected errors: $unexpectedErrors")
        assertEquals(1, successCount.get(), "Exactly one login must succeed")
        assertEquals(threadCount - 1, alreadyUsedCount.get(), "All other attempts must fail with 'already used'")
    }

    @Test
    fun `N users with separate codes logging in simultaneously must all succeed`() {
        val n = 10
        val phones = (1..n).map { "+790000000$it" }

        phones.forEach { p ->
            userRepository.save(User(fullName = "User $p", phone = p))
            smsCodeRepository.save(
                SmsCode(
                    phone = p,
                    code = OtpHasher.hash(p, rawCode),
                    expiresAt = clock.instant().plusSeconds(300)
                )
            )
        }

        val startLatch = CountDownLatch(1)
        val successCount = AtomicInteger(0)
        val errors = mutableListOf<Throwable>()
        val executor = Executors.newFixedThreadPool(n)

        phones.forEach { p ->
            executor.submit {
                startLatch.await()
                try {
                    useCase.login(p, rawCode)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    synchronized(errors) { errors.add(e) }
                }
            }
        }

        startLatch.countDown()
        executor.shutdown()
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS))

        assertTrue(errors.isEmpty(), "No errors expected for distinct users: $errors")
        assertEquals(n, successCount.get(), "All $n users must login successfully")
    }

    @Test
    fun `used code cannot be replayed even after a successful login`() {
        userRepository.save(User(fullName = "Ivan Petrov", phone = phone))
        smsCodeRepository.save(
            SmsCode(
                phone = phone,
                code = OtpHasher.hash(phone, rawCode),
                expiresAt = clock.instant().plusSeconds(300)
            )
        )

        useCase.login(phone, rawCode)  // первый успешный вход

        // Попытка повторно использовать тот же код
        try {
            useCase.login(phone, rawCode)
            error("Expected IllegalArgumentException but no exception was thrown")
        } catch (e: IllegalArgumentException) {
            assertTrue(
                e.message?.contains("used") == true || e.message?.contains("expired") == true,
                "Must fail with 'already used' or 'expired', but got: ${e.message}"
            )
        }
    }
}

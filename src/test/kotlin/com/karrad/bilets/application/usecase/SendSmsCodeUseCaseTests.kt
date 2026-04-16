package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.repository.SmsCodeRepository
import com.karrad.bilets.infrastructure.sms.InMemorySmsRateLimiter
import com.karrad.bilets.infrastructure.sms.MockSmsGateway
import com.karrad.bilets.support.MutableClock
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@SpringJUnitConfig(ApplicationServicesTestConfig::class)
@Import(SendSmsCodeUseCase::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SendSmsCodeUseCaseTests {

    @Autowired lateinit var smsCodeRepository: SmsCodeRepository
    @Autowired lateinit var mockSmsGateway: MockSmsGateway
    @Autowired lateinit var mutableClock: MutableClock

    @Test
    fun `should send code and store it`() {
        val useCase = SendSmsCodeUseCase(smsCodeRepository, mockSmsGateway, mutableClock, InMemorySmsRateLimiter()) { "123456" }

        useCase.send("+79001234567")

        val code = smsCodeRepository.findLatestByPhone("+79001234567")
        assertNotNull(code)
        assertEquals("123456", code.code)
        assertEquals("123456", mockSmsGateway.sentCodes["+79001234567"])
    }

    @Test
    fun `should store code with correct expiry`() {
        val useCase = SendSmsCodeUseCase(smsCodeRepository, mockSmsGateway, mutableClock, InMemorySmsRateLimiter()) { "654321" }

        useCase.send("+79001234567")

        val code = smsCodeRepository.findLatestByPhone("+79001234567")
        assertNotNull(code)
        val expectedExpiry = mutableClock.instant().plusSeconds(SendSmsCodeUseCase.CODE_TTL_SECONDS)
        assertEquals(expectedExpiry, code.expiresAt)
    }

    @Test
    fun `should reject blank phone`() {
        val useCase = SendSmsCodeUseCase(smsCodeRepository, mockSmsGateway, mutableClock, InMemorySmsRateLimiter())

        assertFailsWith<IllegalArgumentException> { useCase.send("  ") }
    }

    @Test
    fun `should reject second send to same phone within rate limit window`() {
        val useCase = SendSmsCodeUseCase(smsCodeRepository, mockSmsGateway, mutableClock, InMemorySmsRateLimiter())

        useCase.send("+79001234567")
        assertFailsWith<IllegalStateException> { useCase.send("+79001234567") }
    }

    @Test
    fun `should allow send after rate limit window expires`() {
        val useCase = SendSmsCodeUseCase(smsCodeRepository, mockSmsGateway, mutableClock, InMemorySmsRateLimiter())

        useCase.send("+79001234567")
        mutableClock.advanceByMinutes(2)
        useCase.send("+79001234567")

        assertNotNull(smsCodeRepository.findLatestByPhone("+79001234567"))
    }

    @Test
    fun `should reject send after hourly limit is reached`() {
        val useCase = SendSmsCodeUseCase(smsCodeRepository, mockSmsGateway, mutableClock, InMemorySmsRateLimiter())
        val phone = "+79009999999"

        // отправляем MAX_REQUESTS_PER_HOUR раз с минутными паузами
        repeat(SendSmsCodeUseCase.MAX_REQUESTS_PER_HOUR) {
            useCase.send(phone)
            mutableClock.advanceByMinutes(2)
        }

        assertFailsWith<IllegalStateException> { useCase.send(phone) }
    }

    @Test
    fun `should allow send after hourly window slides`() {
        val useCase = SendSmsCodeUseCase(smsCodeRepository, mockSmsGateway, mutableClock, InMemorySmsRateLimiter())
        val phone = "+79008888888"

        repeat(SendSmsCodeUseCase.MAX_REQUESTS_PER_HOUR) {
            useCase.send(phone)
            mutableClock.advanceByMinutes(2)
        }

        // прокручиваем время за границу часового окна
        mutableClock.advanceByMinutes(60)
        useCase.send(phone)

        assertNotNull(smsCodeRepository.findLatestByPhone(phone))
    }
}

package com.karrad.bilets.config

import com.karrad.bilets.domain.security.BearerTokenRateLimiter
import com.karrad.bilets.domain.sms.SmsRateLimiter
import com.karrad.bilets.infrastructure.security.NoOpBearerTokenRateLimiter
import com.karrad.bilets.infrastructure.sms.NoOpSmsRateLimiter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

/**
 * Конфигурация для профиля loadtest.
 *
 * Активируется через: --spring.profiles.active=loadtest
 *
 * Отличия от dev-режима:
 *  - SMS rate limiter отключён (NoOp) — можно слать коды без ограничений
 *  - Bearer token rate limiter отключён (NoOp) — нет блокировки по IP
 *  - SMS-код фиксирован = 123456 (через sms.fixed-code в application-loadtest.properties)
 */
@Configuration
@Profile("loadtest")
class LoadTestConfig {

    @Bean
    @Primary
    fun loadTestSmsRateLimiter(): SmsRateLimiter = NoOpSmsRateLimiter()

    @Bean
    @Primary
    fun loadTestBearerTokenRateLimiter(): BearerTokenRateLimiter = NoOpBearerTokenRateLimiter()
}

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
 * Конфигурация devstack-профиля.
 *
 * Активируется через: SPRING_PROFILES_ACTIVE=devstack
 *
 * - SMS rate limiter отключён (NoOp) — без ограничений, код фиксирован = 123456
 * - Bearer token rate limiter отключён (NoOp)
 * - MockPaymentGateway активируется автоматически (profile !prod)
 * - MockSmsGateway активируется автоматически (RepositoryConfig, ConditionalOnMissingBean)
 */
@Configuration
@Profile("devstack")
class DevStackConfig {

    @Bean
    @Primary
    fun devStackSmsRateLimiter(): SmsRateLimiter = NoOpSmsRateLimiter()

    @Bean
    @Primary
    fun devStackBearerTokenRateLimiter(): BearerTokenRateLimiter = NoOpBearerTokenRateLimiter()
}

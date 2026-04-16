package com.karrad.bilets.config

import com.karrad.bilets.application.transaction.OrderFlowTransactionManager
import com.karrad.bilets.domain.repository.CategoryRepository
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.LayoutTemplateRepository
import com.karrad.bilets.domain.repository.PaymentAttemptRepository
import com.karrad.bilets.domain.repository.PaymentCallbackAuditRepository
import com.karrad.bilets.domain.repository.OrganizationApplicationRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.OrderRepository
import com.karrad.bilets.domain.repository.OrderInventoryRepository
import com.karrad.bilets.domain.repository.TicketRepository
import com.karrad.bilets.domain.repository.UserEventVisitRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.domain.repository.CityRepository
import com.karrad.bilets.domain.repository.VenueAccessGrantRepository
import com.karrad.bilets.domain.repository.FavoriteEventRepository
import com.karrad.bilets.domain.repository.VenueRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryCategoryRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryFavoriteEventRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryVenueAccessGrantRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryCityRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryEventInventoryPlanRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryEventRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryLayoutTemplateRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryPaymentAttemptRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryPaymentCallbackAuditRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryOrganizationApplicationRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryOrganizationMemberRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryOrganizationRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryOrderRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryOrderInventoryRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryTicketRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryUserEventVisitRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryUserRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryAuthTokenRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemorySmsCodeRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryVenueRepository
import com.karrad.bilets.domain.security.BearerTokenRateLimiter
import com.karrad.bilets.domain.sms.SmsRateLimiter
import com.karrad.bilets.infrastructure.security.InMemoryBearerTokenRateLimiter
import com.karrad.bilets.infrastructure.sms.InMemorySmsRateLimiter
import com.karrad.bilets.infrastructure.sms.MockSmsGateway
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.SmsCodeRepository
import com.karrad.bilets.domain.sms.SmsGateway
import com.karrad.bilets.infrastructure.transaction.NoOpOrderFlowTransactionManager
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class RepositoryConfig {

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = true)
    fun categoryRepository(): CategoryRepository = InMemoryCategoryRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = true)
    fun organizationRepository(): OrganizationRepository = InMemoryOrganizationRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = true)
    fun organizationApplicationRepository(): OrganizationApplicationRepository = InMemoryOrganizationApplicationRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = true)
    fun organizationMemberRepository(): OrganizationMemberRepository = InMemoryOrganizationMemberRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = true)
    fun userRepository(): UserRepository = InMemoryUserRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = true)
    fun userEventVisitRepository(): UserEventVisitRepository = InMemoryUserEventVisitRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = true)
    fun venueRepository(): VenueRepository = InMemoryVenueRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = true)
    fun venueAccessGrantRepository(): VenueAccessGrantRepository = InMemoryVenueAccessGrantRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = true)
    fun layoutTemplateRepository(): LayoutTemplateRepository = InMemoryLayoutTemplateRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = true)
    fun paymentAttemptRepository(): PaymentAttemptRepository = InMemoryPaymentAttemptRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = true)
    fun paymentCallbackAuditRepository(): PaymentCallbackAuditRepository = InMemoryPaymentCallbackAuditRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = true)
    fun eventRepository(venueRepository: VenueRepository): EventRepository = InMemoryEventRepository(venueRepository)

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = true)
    fun eventInventoryPlanRepository(): EventInventoryPlanRepository = InMemoryEventInventoryPlanRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = true)
    fun orderRepository(): OrderRepository = InMemoryOrderRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = true)
    fun orderInventoryRepository(
        eventInventoryPlanRepository: EventInventoryPlanRepository
    ): OrderInventoryRepository = InMemoryOrderInventoryRepository(eventInventoryPlanRepository)

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = true)
    fun ticketRepository(): TicketRepository = InMemoryTicketRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = true)
    fun smsCodeRepository(): SmsCodeRepository = InMemorySmsCodeRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = true)
    fun authTokenRepository(): AuthTokenRepository = InMemoryAuthTokenRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = true)
    fun cityRepository(): CityRepository = InMemoryCityRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = true)
    fun favoriteEventRepository(): FavoriteEventRepository = InMemoryFavoriteEventRepository()

    @Bean
    @Profile("!prod")
    @ConditionalOnMissingBean(SmsGateway::class)
    fun smsGateway(): SmsGateway = MockSmsGateway()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = true)
    fun orderFlowTransactionManager(): OrderFlowTransactionManager = NoOpOrderFlowTransactionManager()

    @Bean
    @ConditionalOnMissingBean(BearerTokenRateLimiter::class)
    fun bearerTokenRateLimiter(): BearerTokenRateLimiter = InMemoryBearerTokenRateLimiter()

    @Bean
    @ConditionalOnMissingBean(SmsRateLimiter::class)
    fun smsRateLimiter(): SmsRateLimiter = InMemorySmsRateLimiter()
}

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
import com.karrad.bilets.domain.repository.RefreshTokenRepository
import com.karrad.bilets.domain.repository.VenueApplicationRepository
import com.karrad.bilets.domain.repository.VenueRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryCategoryRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryFavoriteEventRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryVenueAccessGrantRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryVenueApplicationRepository
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
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryRefreshTokenRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemorySmsCodeRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryVenueRepository
import com.karrad.bilets.domain.push.PushNotificationGateway
import com.karrad.bilets.domain.repository.AdminCredentialRepository
import com.karrad.bilets.domain.repository.EventPhotoRepository
import com.karrad.bilets.domain.repository.PushTokenRepository
import com.karrad.bilets.domain.repository.SpacePriceProfileRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryAdminCredentialRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryEventPhotoRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryPushTokenRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemorySpacePriceProfileRepository
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.karrad.bilets.infrastructure.push.FcmPushNotificationGateway
import com.karrad.bilets.infrastructure.push.MockPushNotificationGateway
import com.karrad.bilets.application.lock.EventLockManager
import com.karrad.bilets.domain.security.BearerTokenRateLimiter
import com.karrad.bilets.domain.sms.SmsRateLimiter
import com.karrad.bilets.infrastructure.lock.InMemoryEventLockManager
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
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = false)
    fun categoryRepository(): CategoryRepository = InMemoryCategoryRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = false)
    fun organizationRepository(): OrganizationRepository = InMemoryOrganizationRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = false)
    fun organizationApplicationRepository(): OrganizationApplicationRepository = InMemoryOrganizationApplicationRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = false)
    fun organizationMemberRepository(): OrganizationMemberRepository = InMemoryOrganizationMemberRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = false)
    fun userRepository(): UserRepository = InMemoryUserRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = false)
    fun userEventVisitRepository(): UserEventVisitRepository = InMemoryUserEventVisitRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = false)
    fun venueRepository(): VenueRepository = InMemoryVenueRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = false)
    fun venueAccessGrantRepository(): VenueAccessGrantRepository = InMemoryVenueAccessGrantRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = false)
    fun layoutTemplateRepository(): LayoutTemplateRepository = InMemoryLayoutTemplateRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = false)
    fun paymentAttemptRepository(): PaymentAttemptRepository = InMemoryPaymentAttemptRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = false)
    fun paymentCallbackAuditRepository(): PaymentCallbackAuditRepository = InMemoryPaymentCallbackAuditRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = false)
    fun eventRepository(venueRepository: VenueRepository): EventRepository = InMemoryEventRepository(venueRepository)

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = false)
    fun eventInventoryPlanRepository(): EventInventoryPlanRepository = InMemoryEventInventoryPlanRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = false)
    fun orderRepository(): OrderRepository = InMemoryOrderRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = false)
    fun orderInventoryRepository(
        eventInventoryPlanRepository: EventInventoryPlanRepository
    ): OrderInventoryRepository = InMemoryOrderInventoryRepository(eventInventoryPlanRepository)

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = false)
    fun ticketRepository(): TicketRepository = InMemoryTicketRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = false)
    fun smsCodeRepository(): SmsCodeRepository = InMemorySmsCodeRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = false)
    fun authTokenRepository(): AuthTokenRepository = InMemoryAuthTokenRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = false)
    fun cityRepository(): CityRepository = InMemoryCityRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = false)
    fun favoriteEventRepository(): FavoriteEventRepository = InMemoryFavoriteEventRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = false)
    fun venueApplicationRepository(): VenueApplicationRepository = InMemoryVenueApplicationRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = false)
    fun refreshTokenRepository(): RefreshTokenRepository = InMemoryRefreshTokenRepository()

    @Bean
    @Profile("!prod")
    @ConditionalOnMissingBean(SmsGateway::class)
    fun smsGateway(): SmsGateway = MockSmsGateway()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = false)
    fun orderFlowTransactionManager(): OrderFlowTransactionManager = NoOpOrderFlowTransactionManager()

    @Bean
    @ConditionalOnMissingBean(BearerTokenRateLimiter::class)
    fun bearerTokenRateLimiter(): BearerTokenRateLimiter = InMemoryBearerTokenRateLimiter()

    @Bean
    @ConditionalOnMissingBean(SmsRateLimiter::class)
    fun smsRateLimiter(): SmsRateLimiter = InMemorySmsRateLimiter()

    @Bean
    @ConditionalOnMissingBean(EventLockManager::class)
    fun eventLockManager(): EventLockManager = InMemoryEventLockManager()

    @Bean
    @ConditionalOnMissingBean(AdminCredentialRepository::class)
    fun adminCredentialRepository(): AdminCredentialRepository = InMemoryAdminCredentialRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = false)
    fun spacePriceProfileRepository(): SpacePriceProfileRepository = InMemorySpacePriceProfileRepository()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = false)
    fun pushTokenRepository(): PushTokenRepository = InMemoryPushTokenRepository()

    @Bean
    @ConditionalOnProperty(name = ["push.fcm.enabled"], havingValue = "true")
    fun firebaseMessaging(): FirebaseMessaging {
        val credentialsPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS")
        val options = if (credentialsPath != null) {
            FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .build()
        } else {
            FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .build()
        }
        val app = if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
        } else {
            FirebaseApp.getInstance()
        }
        return FirebaseMessaging.getInstance(app)
    }

    @Bean
    @ConditionalOnProperty(name = ["push.fcm.enabled"], havingValue = "true")
    fun fcmPushNotificationGateway(fcm: FirebaseMessaging): PushNotificationGateway =
        FcmPushNotificationGateway(fcm)

    @Bean
    @ConditionalOnMissingBean(PushNotificationGateway::class)
    fun pushNotificationGateway(): PushNotificationGateway = MockPushNotificationGateway()

    @Bean
    @ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = false)
    fun eventPhotoRepository(): EventPhotoRepository = InMemoryEventPhotoRepository()
}

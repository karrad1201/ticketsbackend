package com.karrad.bilets.application.service

import com.karrad.bilets.application.lock.EventLockManager
import com.karrad.bilets.config.PurchaseProperties
import com.karrad.bilets.domain.payment.PaymentGateway
import com.karrad.bilets.domain.repository.CategoryRepository
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.LayoutTemplateRepository
import com.karrad.bilets.domain.repository.OrganizationApplicationRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.OrderRepository
import com.karrad.bilets.domain.repository.TicketRepository
import com.karrad.bilets.domain.repository.UserEventVisitRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.domain.repository.VenueRepository
import com.karrad.bilets.infrastructure.lock.InMemoryEventLockManager
import com.karrad.bilets.infrastructure.payment.MockPaymentGateway
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryCategoryRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryEventInventoryPlanRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryEventRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryLayoutTemplateRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryOrganizationApplicationRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryOrganizationMemberRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryOrganizationRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryOrderRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryTicketRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryUserEventVisitRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryUserRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryVenueRepository
import com.karrad.bilets.support.YamlPropertySourceFactory
import com.karrad.bilets.support.MutableClock
import org.springframework.context.annotation.Bean
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.PropertySource
import java.time.Clock
import java.time.Instant

@TestConfiguration
@EnableConfigurationProperties(PurchaseProperties::class)
@PropertySource(value = ["classpath:application.yml"], factory = YamlPropertySourceFactory::class)
class ApplicationServicesTestConfig {

    @Bean
    fun categoryRepository(): CategoryRepository = InMemoryCategoryRepository()

    @Bean
    fun organizationRepository(): OrganizationRepository = InMemoryOrganizationRepository()

    @Bean
    fun organizationApplicationRepository(): OrganizationApplicationRepository = InMemoryOrganizationApplicationRepository()

    @Bean
    fun organizationMemberRepository(): OrganizationMemberRepository = InMemoryOrganizationMemberRepository()

    @Bean
    fun userRepository(): UserRepository = InMemoryUserRepository()

    @Bean
    fun userEventVisitRepository(): UserEventVisitRepository = InMemoryUserEventVisitRepository()

    @Bean
    fun venueRepository(): VenueRepository = InMemoryVenueRepository()

    @Bean
    fun layoutTemplateRepository(): LayoutTemplateRepository = InMemoryLayoutTemplateRepository()

    @Bean
    fun eventRepository(): EventRepository = InMemoryEventRepository()

    @Bean
    fun eventInventoryPlanRepository(): EventInventoryPlanRepository = InMemoryEventInventoryPlanRepository()

    @Bean
    fun orderRepository(): OrderRepository = InMemoryOrderRepository()

    @Bean
    fun ticketRepository(): TicketRepository = InMemoryTicketRepository()

    @Bean
    fun paymentGateway(): PaymentGateway = MockPaymentGateway()

    @Bean
    fun mockPaymentGateway(): MockPaymentGateway = paymentGateway() as MockPaymentGateway

    @Bean
    fun eventLockManager(): EventLockManager = InMemoryEventLockManager()

    @Bean
    fun clock(): Clock = MutableClock(Instant.parse("2026-03-23T00:00:00Z"))

    @Bean
    fun mutableClock(): MutableClock = clock() as MutableClock

    @Bean
    fun categoryService(categoryRepository: CategoryRepository): CategoryService = CategoryService(categoryRepository)

    @Bean
    fun venueService(venueRepository: VenueRepository): VenueService = VenueService(venueRepository)

    @Bean
    fun organizationService(organizationRepository: OrganizationRepository): OrganizationService =
        OrganizationService(organizationRepository)

    @Bean
    fun organizationApplicationService(organizationApplicationRepository: OrganizationApplicationRepository): OrganizationApplicationService =
        OrganizationApplicationService(organizationApplicationRepository)

    @Bean
    fun organizationMemberService(organizationMemberRepository: OrganizationMemberRepository): OrganizationMemberService =
        OrganizationMemberService(organizationMemberRepository)

    @Bean
    fun userService(userRepository: UserRepository): UserService = UserService(userRepository)

    @Bean
    fun userEventVisitService(userEventVisitRepository: UserEventVisitRepository): UserEventVisitService =
        UserEventVisitService(userEventVisitRepository)

    @Bean
    fun layoutTemplateService(layoutTemplateRepository: LayoutTemplateRepository): LayoutTemplateService =
        LayoutTemplateService(layoutTemplateRepository)

    @Bean
    fun eventService(eventRepository: EventRepository): EventService = EventService(eventRepository)

    @Bean
    fun inventoryPlanService(eventInventoryPlanRepository: EventInventoryPlanRepository): InventoryPlanService =
        InventoryPlanService(eventInventoryPlanRepository)
}

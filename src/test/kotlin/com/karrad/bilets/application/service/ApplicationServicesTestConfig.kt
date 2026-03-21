package com.karrad.bilets.application.service

import com.karrad.bilets.domain.repository.CategoryRepository
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.LayoutTemplateRepository
import com.karrad.bilets.domain.repository.OrganizationApplicationRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.domain.repository.VenueRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryCategoryRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryEventInventoryPlanRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryEventRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryLayoutTemplateRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryOrganizationApplicationRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryOrganizationMemberRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryOrganizationRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryUserRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryVenueRepository
import org.springframework.context.annotation.Bean
import org.springframework.boot.test.context.TestConfiguration

@TestConfiguration
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
    fun venueRepository(): VenueRepository = InMemoryVenueRepository()

    @Bean
    fun layoutTemplateRepository(): LayoutTemplateRepository = InMemoryLayoutTemplateRepository()

    @Bean
    fun eventRepository(): EventRepository = InMemoryEventRepository()

    @Bean
    fun eventInventoryPlanRepository(): EventInventoryPlanRepository = InMemoryEventInventoryPlanRepository()

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
    fun layoutTemplateService(layoutTemplateRepository: LayoutTemplateRepository): LayoutTemplateService =
        LayoutTemplateService(layoutTemplateRepository)

    @Bean
    fun eventService(eventRepository: EventRepository): EventService = EventService(eventRepository)

    @Bean
    fun inventoryPlanService(eventInventoryPlanRepository: EventInventoryPlanRepository): InventoryPlanService =
        InventoryPlanService(eventInventoryPlanRepository)
}

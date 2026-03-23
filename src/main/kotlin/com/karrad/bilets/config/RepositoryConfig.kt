package com.karrad.bilets.config

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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RepositoryConfig {

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
}

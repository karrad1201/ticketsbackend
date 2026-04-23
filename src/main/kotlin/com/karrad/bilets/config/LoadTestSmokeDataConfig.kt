package com.karrad.bilets.config

import com.karrad.bilets.domain.entity.AuthToken
import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.TicketType
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.CategoryRepository
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.domain.repository.VenueRepository
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.time.Clock
import java.time.Duration
import java.util.UUID

@Configuration
@Profile("loadtest")
class LoadTestSmokeDataConfig {

    @Bean
    @ConditionalOnProperty(
        prefix = "loadtest.smoke-seed",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true
    )
    fun loadTestSmokeDataSeeder(
        categoryRepository: CategoryRepository,
        organizationRepository: OrganizationRepository,
        organizationMemberRepository: OrganizationMemberRepository,
        venueRepository: VenueRepository,
        eventRepository: EventRepository,
        eventInventoryPlanRepository: EventInventoryPlanRepository,
        userRepository: UserRepository,
        authTokenRepository: AuthTokenRepository,
        clock: Clock
    ) = ApplicationRunner {
        val now = clock.instant()

        val category = categoryRepository.findByCode("k6-smoke-concerts") ?: categoryRepository.save(
            Category(
                id = SmokeIds.categoryId,
                code = "k6-smoke-concerts",
                label = "K6 Smoke Concerts"
            )
        )
        val organization = organizationRepository.findByCode("k6-smoke-org") ?: organizationRepository.save(
            Organization(
                id = SmokeIds.organizationId,
                code = "k6-smoke-org",
                name = "K6 Smoke Org"
            )
        )
        val admin = userRepository.findByPhone("+79990000001") ?: userRepository.save(
            User(
                id = SmokeIds.adminUserId,
                fullName = "K6 Smoke Admin",
                phone = "+79990000001",
                role = UserRole.ADMIN
            )
        )
        val user = userRepository.findByPhone("+79990000002") ?: userRepository.save(
            User(
                id = SmokeIds.userId,
                fullName = "K6 Smoke User",
                phone = "+79990000002"
            )
        )
        organizationMemberRepository.save(
            OrganizationMember(
                id = SmokeIds.organizationMemberId,
                organizationId = organization.id,
                userId = admin.id,
                role = OrganizationMemberRole.OWNER
            )
        )

        val venue = venueRepository.save(
            Venue(
                id = SmokeIds.venueId,
                label = "K6 Smoke Venue",
                city = City(label = "Москва", subject = Subject("Москва")),
                organizationId = organization.id,
                address = "ул. Smoke, 1"
            )
        )
        SmokeIds.events.forEachIndexed { index, seededEvent ->
            val event = eventRepository.save(
                Event(
                    id = seededEvent.eventId,
                    label = "K6 Smoke Concert ${index + 1}",
                    description = "Stable smoke-test event ${index + 1} for k6",
                    venueId = venue.id,
                    categoryId = category.id,
                    organizationId = organization.id,
                    time = now.plus(Duration.ofDays(30 + index.toLong())),
                    minPrice = 1000,
                    hasSeatMap = false
                )
            )
            eventInventoryPlanRepository.save(
                EventInventoryPlan.generalAdmission(
                    event = event,
                    ticketTypes = listOf(
                        TicketType(
                            id = seededEvent.standardTicketTypeId,
                            label = "Standard",
                            price = 1000,
                            quota = 10000
                        ),
                        TicketType(
                            id = seededEvent.vipTicketTypeId,
                            label = "VIP",
                            price = 5000,
                            quota = 1000
                        )
                    )
                )
            )
        }

        upsertToken(authTokenRepository, "k6-smoke-admin-token", admin.id, now)
        upsertToken(authTokenRepository, "k6-smoke-user-token", user.id, now)
    }

    private fun upsertToken(
        authTokenRepository: AuthTokenRepository,
        token: String,
        userId: UUID,
        now: java.time.Instant
    ) {
        authTokenRepository.deleteByToken(token)
        authTokenRepository.save(
            AuthToken(
                token = token,
                userId = userId,
                createdAt = now,
                expiresAt = now.plus(Duration.ofDays(7))
            )
        )
    }

    private object SmokeIds {
        val categoryId: UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        val organizationId: UUID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        val organizationMemberId: UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")
        val adminUserId: UUID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd")
        val userId: UUID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee")
        val venueId: UUID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff")
        val events = listOf(
            SeededEvent("11111111-1111-1111-1111-111111111111", "22222222-2222-2222-2222-222222222222", "33333333-3333-3333-3333-333333333333"),
            SeededEvent("11111111-1111-1111-1111-111111111112", "22222222-2222-2222-2222-222222222223", "33333333-3333-3333-3333-333333333334"),
            SeededEvent("11111111-1111-1111-1111-111111111113", "22222222-2222-2222-2222-222222222224", "33333333-3333-3333-3333-333333333335"),
            SeededEvent("11111111-1111-1111-1111-111111111114", "22222222-2222-2222-2222-222222222225", "33333333-3333-3333-3333-333333333336"),
            SeededEvent("11111111-1111-1111-1111-111111111115", "22222222-2222-2222-2222-222222222226", "33333333-3333-3333-3333-333333333337"),
            SeededEvent("11111111-1111-1111-1111-111111111116", "22222222-2222-2222-2222-222222222227", "33333333-3333-3333-3333-333333333338"),
            SeededEvent("11111111-1111-1111-1111-111111111117", "22222222-2222-2222-2222-222222222228", "33333333-3333-3333-3333-333333333339"),
            SeededEvent("11111111-1111-1111-1111-111111111118", "22222222-2222-2222-2222-222222222229", "33333333-3333-3333-3333-33333333333a")
        )
    }

    private data class SeededEvent(
        val eventId: UUID,
        val standardTicketTypeId: UUID,
        val vipTicketTypeId: UUID
    ) {
        constructor(eventId: String, standardTicketTypeId: String, vipTicketTypeId: String) : this(
            UUID.fromString(eventId),
            UUID.fromString(standardTicketTypeId),
            UUID.fromString(vipTicketTypeId)
        )
    }
}

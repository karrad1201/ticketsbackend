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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.time.Clock
import java.time.Duration
import java.util.UUID

/**
 * Предзасеянные данные для devstack-профиля.
 *
 * ## Аккаунты
 *
 * | Роль           | Телефон        | Токен                  | SMS-код |
 * |----------------|----------------|------------------------|---------|
 * | ADMIN          | +79991000001   | devstack-admin-token   | 123456  |
 * | Орг. OWNER     | +79991000002   | devstack-owner-token   | 123456  |
 * | Орг. STAFF     | +79991000003   | devstack-staff-token   | 123456  |
 * | Обычный user   | +79991000004   | devstack-user-token    | 123456  |
 *
 * ## Организация
 * - Название: "DevStack Fest"
 * - Код: `devstack-org`
 *
 * ## Площадка
 * - Название: "Арена DevStack"
 * - Город: Москва, адрес: ул. Тестовая, 1
 *
 * ## Мероприятия (3 события)
 * - "DevStack Open Air" — через 7 дней, 500 мест по 1 000 руб.
 * - "DevStack Conference" — через 14 дней, 200 мест по 2 500 руб. + VIP 50 мест по 7 500 руб.
 * - "DevStack After Party" — через 30 дней, 100 мест по 500 руб.
 */
@Configuration
@Profile("devstack")
class DevStackSeedDataConfig {

    @Bean
    fun devStackSeeder(
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

        // ── Category ──────────────────────────────────────────────────────────
        val category = categoryRepository.findByCode("devstack-concerts")
            ?: categoryRepository.save(
                Category(
                    id = Ids.categoryId,
                    code = "devstack-concerts",
                    label = "DevStack Concerts"
                )
            )

        // ── Organization ──────────────────────────────────────────────────────
        val org = organizationRepository.findByCode("devstack-org")
            ?: organizationRepository.save(
                Organization(
                    id = Ids.orgId,
                    code = "devstack-org",
                    name = "DevStack Fest"
                )
            )

        // ── Users ─────────────────────────────────────────────────────────────
        val admin = userRepository.findByPhone("+79991000001")
            ?: userRepository.save(
                User(
                    id = Ids.adminUserId,
                    fullName = "DevStack Admin",
                    phone = "+79991000001",
                    role = UserRole.ADMIN
                )
            )
        val owner = userRepository.findByPhone("+79991000002")
            ?: userRepository.save(
                User(
                    id = Ids.ownerUserId,
                    fullName = "DevStack Owner",
                    phone = "+79991000002"
                )
            )
        val staff = userRepository.findByPhone("+79991000003")
            ?: userRepository.save(
                User(
                    id = Ids.staffUserId,
                    fullName = "DevStack Staff",
                    phone = "+79991000003"
                )
            )
        userRepository.findByPhone("+79991000004")
            ?: userRepository.save(
                User(
                    id = Ids.regularUserId,
                    fullName = "DevStack User",
                    phone = "+79991000004"
                )
            )

        // ── Owner membership (без venueId — до создания venue) ───────────────
        organizationMemberRepository.save(
            OrganizationMember(
                id = Ids.ownerMemberId,
                organizationId = org.id,
                userId = owner.id,
                role = OrganizationMemberRole.OWNER
            )
        )

        // ── Venue ─────────────────────────────────────────────────────────────
        val venue = venueRepository.save(
            Venue(
                id = Ids.venueId,
                label = "Арена DevStack",
                city = City(label = "Москва", subject = Subject("Москва")),
                organizationId = org.id,
                address = "ул. Тестовая, 1"
            )
        )

        // ── Staff membership (после venue — FK constraint) ────────────────────
        organizationMemberRepository.save(
            OrganizationMember(
                id = Ids.staffMemberId,
                organizationId = org.id,
                userId = staff.id,
                role = OrganizationMemberRole.STAFF,
                venueId = venue.id
            )
        )

        // ── Events ────────────────────────────────────────────────────────────
        val event1 = eventRepository.save(
            Event(
                id = Ids.event1Id,
                label = "DevStack Open Air",
                description = "Летний open-air фестиваль для разработчиков",
                venueId = venue.id,
                categoryId = category.id,
                organizationId = org.id,
                time = now.plus(Duration.ofDays(7)),
                minPrice = 1000,
                hasSeatMap = false
            )
        )
        eventInventoryPlanRepository.save(
            EventInventoryPlan.generalAdmission(
                event = event1,
                ticketTypes = listOf(
                    TicketType(id = Ids.event1TicketTypeId, label = "Стандарт", price = 1000, quota = 500)
                )
            )
        )

        val event2 = eventRepository.save(
            Event(
                id = Ids.event2Id,
                label = "DevStack Conference",
                description = "Ежегодная конференция по разработке ПО",
                venueId = venue.id,
                categoryId = category.id,
                organizationId = org.id,
                time = now.plus(Duration.ofDays(14)),
                minPrice = 2500,
                hasSeatMap = false
            )
        )
        eventInventoryPlanRepository.save(
            EventInventoryPlan.generalAdmission(
                event = event2,
                ticketTypes = listOf(
                    TicketType(id = Ids.event2StandardTicketTypeId, label = "Стандарт", price = 2500, quota = 200),
                    TicketType(id = Ids.event2VipTicketTypeId, label = "VIP", price = 7500, quota = 50)
                )
            )
        )

        val event3 = eventRepository.save(
            Event(
                id = Ids.event3Id,
                label = "DevStack After Party",
                description = "Закрытая вечеринка для участников конференции",
                venueId = venue.id,
                categoryId = category.id,
                organizationId = org.id,
                time = now.plus(Duration.ofDays(30)),
                minPrice = 500,
                hasSeatMap = false
            )
        )
        eventInventoryPlanRepository.save(
            EventInventoryPlan.generalAdmission(
                event = event3,
                ticketTypes = listOf(
                    TicketType(id = Ids.event3TicketTypeId, label = "Стандарт", price = 500, quota = 100)
                )
            )
        )

        // ── Auth tokens ───────────────────────────────────────────────────────
        upsertToken(authTokenRepository, "devstack-admin-token", admin.id, now)
        upsertToken(authTokenRepository, "devstack-owner-token", owner.id, now)
        upsertToken(authTokenRepository, "devstack-staff-token", staff.id, now)
        upsertToken(authTokenRepository, "devstack-user-token", Ids.regularUserId, now)
    }

    private fun upsertToken(
        repo: AuthTokenRepository,
        token: String,
        userId: UUID,
        now: java.time.Instant
    ) {
        repo.deleteByToken(token)
        repo.save(
            AuthToken(
                token = token,
                userId = userId,
                createdAt = now,
                expiresAt = now.plus(Duration.ofDays(365))
            )
        )
    }

    private object Ids {
        val categoryId: UUID             = UUID.fromString("d5000000-0000-0000-0000-000000000001")
        val orgId: UUID                  = UUID.fromString("d5000000-0000-0000-0000-000000000002")
        val adminUserId: UUID            = UUID.fromString("d5000000-0000-0000-0000-000000000003")
        val ownerUserId: UUID            = UUID.fromString("d5000000-0000-0000-0000-000000000004")
        val staffUserId: UUID            = UUID.fromString("d5000000-0000-0000-0000-000000000005")
        val regularUserId: UUID          = UUID.fromString("d5000000-0000-0000-0000-000000000006")
        val ownerMemberId: UUID          = UUID.fromString("d5000000-0000-0000-0000-000000000007")
        val staffMemberId: UUID          = UUID.fromString("d5000000-0000-0000-0000-000000000008")
        val venueId: UUID                = UUID.fromString("d5000000-0000-0000-0000-000000000009")
        val event1Id: UUID               = UUID.fromString("d5000000-0000-0000-0000-000000000010")
        val event1TicketTypeId: UUID     = UUID.fromString("d5000000-0000-0000-0000-000000000011")
        val event2Id: UUID               = UUID.fromString("d5000000-0000-0000-0000-000000000012")
        val event2StandardTicketTypeId: UUID = UUID.fromString("d5000000-0000-0000-0000-000000000013")
        val event2VipTicketTypeId: UUID  = UUID.fromString("d5000000-0000-0000-0000-000000000014")
        val event3Id: UUID               = UUID.fromString("d5000000-0000-0000-0000-000000000015")
        val event3TicketTypeId: UUID     = UUID.fromString("d5000000-0000-0000-0000-000000000016")
    }
}

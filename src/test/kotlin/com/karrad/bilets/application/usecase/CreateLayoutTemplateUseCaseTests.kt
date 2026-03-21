package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.LayoutTemplate
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.entity.Row
import com.karrad.bilets.domain.entity.Section
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.VenueSpace
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.repository.LayoutTemplateRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.VenueRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringJUnitConfig(ApplicationServicesTestConfig::class)
@Import(CreateLayoutTemplateUseCase::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CreateLayoutTemplateUseCaseTests {

    @Autowired
    lateinit var venueRepository: VenueRepository

    @Autowired
    lateinit var organizationRepository: OrganizationRepository

    @Autowired
    lateinit var organizationMemberRepository: OrganizationMemberRepository

    @Autowired
    lateinit var layoutTemplateRepository: LayoutTemplateRepository

    @Autowired
    lateinit var useCase: CreateLayoutTemplateUseCase

    @Test
    fun `should create layout template when venue space exists`() {
        val actorUserId = UUID.fromString("123e4567-e89b-12d3-a456-426614174513")
        seedOrganizationAccess(actorUserId)
        val venue = demoVenue()
        venueRepository.save(venue)

        val result = useCase.create(
            demoLayoutTemplate(venueSpaceId = venue.spaces.first().id),
            actorUserId
        )

        assertEquals(venue.spaces.first().id, result.venueSpaceId)
        assertNotNull(layoutTemplateRepository.findById(result.id))
    }

    @Test
    fun `should reject layout template creation when venue space does not exist`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.create(
                demoLayoutTemplate(
                    venueSpaceId = UUID.fromString("123e4567-e89b-12d3-a456-426614174501")
                ),
                UUID.fromString("123e4567-e89b-12d3-a456-426614174513")
            )
        }

        assertTrue(exception.message!!.contains("VenueSpace not found"))
    }

    @Test
    fun `should reject layout template creation when actor is not organization member`() {
        organizationRepository.save(demoOrganization())
        venueRepository.save(demoVenue())

        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.create(
                demoLayoutTemplate(venueSpaceId = UUID.fromString("123e4567-e89b-12d3-a456-426614174511")),
                UUID.fromString("123e4567-e89b-12d3-a456-426614174514")
            )
        }

        assertTrue(exception.message!!.contains("is not a member"))
    }

    @Test
    fun `should reject layout template creation when venue has no organization`() {
        venueRepository.save(
            demoVenue().copy(
                organizationId = null,
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614174515")
            )
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.create(
                demoLayoutTemplate(venueSpaceId = UUID.fromString("123e4567-e89b-12d3-a456-426614174511")),
                UUID.fromString("123e4567-e89b-12d3-a456-426614174513")
            )
        }

        assertTrue(exception.message!!.contains("is not attached to an organization"))
    }

    private fun seedOrganizationAccess(actorUserId: UUID) {
        organizationRepository.save(demoOrganization())
        organizationMemberRepository.save(
            OrganizationMember(
                organizationId = demoOrganization().id,
                userId = actorUserId,
                role = OrganizationMemberRole.OWNER
            )
        )
    }

    private fun demoOrganization(): Organization {
        return Organization(
            code = "demo-org",
            name = "Demo Org",
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174509")
        )
    }

    private fun demoVenue(): Venue {
        return Venue(
            label = "Demo Hall",
            city = City(label = "Ekaterinburg", subject = Subject(label = "Sverdlovsk Oblast")),
            organizationId = demoOrganization().id,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174510"),
            spaces = listOf(
                VenueSpace(
                    label = "Main Hall",
                    id = UUID.fromString("123e4567-e89b-12d3-a456-426614174511")
                )
            )
        )
    }

    private fun demoLayoutTemplate(venueSpaceId: UUID): LayoutTemplate {
        return LayoutTemplate(
            venueSpaceId = venueSpaceId,
            label = "Theatre Layout",
            sections = listOf(
                Section(
                    label = "Партер",
                    key = "parter",
                    rows = listOf(
                        Row(label = "Ряд 1", key = "r1", startSeat = 1, endSeat = 3, price = 2000)
                    )
                )
            ),
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174512")
        )
    }
}

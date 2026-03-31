package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.VenueSpace
import com.karrad.bilets.domain.enums.OrganizationMemberRole
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
@Import(CreateVenueUseCase::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CreateVenueUseCaseTests {

    @Autowired
    lateinit var organizationRepository: OrganizationRepository

    @Autowired
    lateinit var organizationMemberRepository: OrganizationMemberRepository

    @Autowired
    lateinit var venueRepository: VenueRepository

    @Autowired
    lateinit var useCase: CreateVenueUseCase

    @Test
    fun `should create venue with spaces`() {
        val actorUserId = UUID.fromString("123e4567-e89b-12d3-a456-426614174613")
        seedOrganizationAccess(actorUserId)
        val venue = demoVenue()

        val result = useCase.create(venue, actorUserId)

        assertEquals("Demo Hall", result.label)
        assertEquals(demoOrganization().id, result.organizationId)
        assertEquals(2, result.spaces.size)
        assertNotNull(venueRepository.findById(result.id))
    }

    @Test
    fun `should reject venue with duplicate space ids`() {
        val actorUserId = UUID.fromString("123e4567-e89b-12d3-a456-426614174613")
        seedOrganizationAccess(actorUserId)
        val duplicateId = UUID.fromString("123e4567-e89b-12d3-a456-426614174601")

        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.create(
                Venue(
                    label = "Broken Hall",
                    city = City(
                        label = "Ekaterinburg",
                        subject = Subject(label = "Sverdlovsk Oblast")
                    ),
                    organizationId = demoOrganization().id,
                    spaces = listOf(
                        VenueSpace(label = "Main Hall", id = duplicateId),
                        VenueSpace(label = "Small Hall", id = duplicateId)
                    )
                ),
                actorUserId
            )
        }

        assertTrue(exception.message!!.contains("unique"))
    }

    @Test
    fun `should reject venue creation when actor is not organization member`() {
        organizationRepository.save(demoOrganization())

        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.create(
                demoVenue(),
                UUID.fromString("123e4567-e89b-12d3-a456-426614174614")
            )
        }

        assertTrue(exception.message!!.contains("is not a member"))
    }

    @Test
    fun `should reject venue creation when organization is missing`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.create(
                demoVenue(),
                UUID.fromString("123e4567-e89b-12d3-a456-426614174615")
            )
        }

        assertTrue(exception.message!!.contains("Organization not found"))
    }

    @Test
    fun `should reject venue when organizationId is null`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.create(
                Venue(
                    label = "No Org Hall",
                    city = City(label = "Ekaterinburg", subject = Subject(label = "Sverdlovsk Oblast")),
                    organizationId = null
                ),
                UUID.fromString("123e4567-e89b-12d3-a456-426614174616")
            )
        }

        assertTrue(exception.message!!.contains("organizationId must be provided"))
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
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174609")
        )
    }

    private fun demoVenue(): Venue {
        return Venue(
            label = "Demo Hall",
            city = City(label = "Ekaterinburg", subject = Subject(label = "Sverdlovsk Oblast")),
            organizationId = demoOrganization().id,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174610"),
            spaces = listOf(
                VenueSpace(
                    label = "Main Hall",
                    id = UUID.fromString("123e4567-e89b-12d3-a456-426614174611")
                ),
                VenueSpace(
                    label = "Small Hall",
                    id = UUID.fromString("123e4567-e89b-12d3-a456-426614174612")
                )
            )
        )
    }
}

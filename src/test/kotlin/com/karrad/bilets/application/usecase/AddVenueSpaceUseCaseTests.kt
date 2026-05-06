package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.VenueSpace
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.enums.VenueSpaceType
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

@SpringJUnitConfig(ApplicationServicesTestConfig::class)
@Import(AddVenueSpaceUseCase::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AddVenueSpaceUseCaseTests {

    @Autowired lateinit var venueRepository: VenueRepository
    @Autowired lateinit var organizationRepository: OrganizationRepository
    @Autowired lateinit var organizationMemberRepository: OrganizationMemberRepository
    @Autowired lateinit var useCase: AddVenueSpaceUseCase

    private val orgId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")
    private val userId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002")
    private val venueId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000003")

    private fun setup() {
        organizationRepository.save(Organization(code = "test-org", name = "Test Org", id = orgId))
        organizationMemberRepository.save(
            OrganizationMember(organizationId = orgId, userId = userId, role = OrganizationMemberRole.OWNER)
        )
        venueRepository.save(
            Venue(
                id = venueId,
                label = "Test Venue",
                city = City(label = "Элиста", subject = Subject(label = "Республика Калмыкия")),
                organizationId = orgId
            )
        )
    }

    @Test
    fun `should add ADMISSION space to venue`() {
        setup()
        val space = VenueSpace(label = "Партер", type = VenueSpaceType.ADMISSION, capacity = 200)

        val result = useCase.add(venueId, space, userId)

        assertEquals("Партер", result.label)
        assertEquals(VenueSpaceType.ADMISSION, result.type)
        assertEquals(200, result.capacity)
        val updated = venueRepository.findById(venueId)!!
        assertEquals(1, updated.spaces.size)
        assertEquals("Партер", updated.spaces.first().label)
    }

    @Test
    fun `should add SEATED space to venue`() {
        setup()
        val space = VenueSpace(label = "Зал А", type = VenueSpaceType.SEATED, capacity = 150)

        val result = useCase.add(venueId, space, userId)

        assertEquals(VenueSpaceType.SEATED, result.type)
        assertEquals(150, result.capacity)
    }

    @Test
    fun `should reject when venue not found`() {
        setup()
        val unknownVenueId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001")

        assertFailsWith<IllegalArgumentException> {
            useCase.add(unknownVenueId, VenueSpace(label = "Space"), userId)
        }
    }

    @Test
    fun `should reject when user has no membership`() {
        setup()
        val stranger = UUID.fromString("cccccccc-0000-0000-0000-000000000001")

        assertFailsWith<SecurityException> {
            useCase.add(venueId, VenueSpace(label = "Space"), stranger)
        }
    }

    @Test
    fun `should reject when user organization does not own the venue`() {
        setup()
        val otherOrgId = UUID.fromString("dddddddd-0000-0000-0000-000000000001")
        val otherUserId = UUID.fromString("dddddddd-0000-0000-0000-000000000002")
        organizationRepository.save(Organization(code = "other", name = "Other", id = otherOrgId))
        organizationMemberRepository.save(
            OrganizationMember(organizationId = otherOrgId, userId = otherUserId, role = OrganizationMemberRole.OWNER)
        )

        assertFailsWith<IllegalArgumentException> {
            useCase.add(venueId, VenueSpace(label = "Space"), otherUserId)
        }
    }
}

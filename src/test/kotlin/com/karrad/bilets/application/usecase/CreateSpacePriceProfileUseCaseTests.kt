package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.InventoryMode
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.entity.SectionPrice
import com.karrad.bilets.domain.entity.SpacePriceProfile
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.TicketTypeTemplate
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.VenueSpace
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.SpacePriceProfileRepository
import com.karrad.bilets.domain.repository.VenueRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@SpringJUnitConfig(ApplicationServicesTestConfig::class)
@Import(CreateSpacePriceProfileUseCase::class, DeleteSpacePriceProfileUseCase::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CreateSpacePriceProfileUseCaseTests {

    @Autowired lateinit var venueRepository: VenueRepository
    @Autowired lateinit var organizationRepository: OrganizationRepository
    @Autowired lateinit var organizationMemberRepository: OrganizationMemberRepository
    @Autowired lateinit var spacePriceProfileRepository: SpacePriceProfileRepository
    @Autowired lateinit var createUseCase: CreateSpacePriceProfileUseCase
    @Autowired lateinit var deleteUseCase: DeleteSpacePriceProfileUseCase

    private val orgId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val venueId = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val spaceId = UUID.fromString("00000000-0000-0000-0000-000000000003")
    private val callerId = UUID.fromString("00000000-0000-0000-0000-000000000004")

    @Test
    fun `should create seated price profile when caller is org member`() {
        seedVenueAndMember()
        val profile = SpacePriceProfile(
            venueSpaceId = spaceId,
            label = "Standard 2025",
            mode = InventoryMode.SEATED,
            sectionPrices = listOf(SectionPrice(sectionKey = "A", price = 1500))
        )

        val saved = createUseCase.create(profile, callerId)

        assertEquals("Standard 2025", saved.label)
        assertEquals(spaceId, saved.venueSpaceId)
        assertEquals(spacePriceProfileRepository.findById(saved.id), saved)
    }

    @Test
    fun `should create GA price profile when caller is org member`() {
        seedVenueAndMember()
        val profile = SpacePriceProfile(
            venueSpaceId = spaceId,
            label = "GA Profile",
            mode = InventoryMode.GENERAL_ADMISSION,
            ticketTypes = listOf(TicketTypeTemplate(label = "Standard", price = 800, quota = 100))
        )

        val saved = createUseCase.create(profile, callerId)

        assertEquals(InventoryMode.GENERAL_ADMISSION, saved.mode)
        assertEquals(1, saved.ticketTypes.size)
    }

    @Test
    fun `should reject creation when venue space does not exist`() {
        val profile = SpacePriceProfile(
            venueSpaceId = UUID.fromString("00000000-0000-0000-0000-000000000099"),
            label = "Profile",
            mode = InventoryMode.SEATED,
            sectionPrices = listOf(SectionPrice("A", 1000))
        )

        assertFailsWith<NoSuchElementException> {
            createUseCase.create(profile, callerId)
        }
    }

    @Test
    fun `should reject creation when caller is not an org member`() {
        seedVenueAndMember()
        val profile = SpacePriceProfile(
            venueSpaceId = spaceId,
            label = "Profile",
            mode = InventoryMode.SEATED,
            sectionPrices = listOf(SectionPrice("A", 1000))
        )
        val stranger = UUID.fromString("00000000-0000-0000-0000-000000000099")

        assertFailsWith<SecurityException> {
            createUseCase.create(profile, stranger)
        }
    }

    @Test
    fun `should reject creation when caller belongs to a different organization`() {
        seedVenueAndMember()
        val otherOrgId = UUID.fromString("00000000-0000-0000-0000-000000000088")
        organizationRepository.save(Organization(code = "other", name = "Other Org", id = otherOrgId))
        val otherMemberId = UUID.fromString("00000000-0000-0000-0000-000000000089")
        organizationMemberRepository.save(
            OrganizationMember(organizationId = otherOrgId, userId = otherMemberId, role = OrganizationMemberRole.OWNER)
        )
        val profile = SpacePriceProfile(
            venueSpaceId = spaceId,
            label = "Profile",
            mode = InventoryMode.SEATED,
            sectionPrices = listOf(SectionPrice("A", 1000))
        )

        val ex = assertFailsWith<IllegalArgumentException> {
            createUseCase.create(profile, otherMemberId)
        }
        assertTrue(ex.message!!.contains("Only the venue's organization"))
    }

    @Test
    fun `should delete price profile when caller is org member`() {
        seedVenueAndMember()
        val profile = spacePriceProfileRepository.save(
            SpacePriceProfile(
                venueSpaceId = spaceId,
                label = "To Delete",
                mode = InventoryMode.SEATED,
                sectionPrices = listOf(SectionPrice("A", 1000))
            )
        )

        deleteUseCase.delete(profile.id, callerId)

        assertEquals(null, spacePriceProfileRepository.findById(profile.id))
    }

    @Test
    fun `should reject deletion when profile does not exist`() {
        assertFailsWith<IllegalArgumentException> {
            deleteUseCase.delete(UUID.fromString("00000000-0000-0000-0000-000000000099"), callerId)
        }
    }

    private fun seedVenueAndMember() {
        organizationRepository.save(Organization(code = "demo-org", name = "Demo Org", id = orgId))
        venueRepository.save(
            Venue(
                label = "Demo Venue",
                city = City(label = "Elista", subject = Subject(label = "Republic of Kalmykia")),
                organizationId = orgId,
                id = venueId,
                spaces = listOf(VenueSpace(label = "Main Hall", id = spaceId))
            )
        )
        organizationMemberRepository.save(
            OrganizationMember(organizationId = orgId, userId = callerId, role = OrganizationMemberRole.OWNER)
        )
    }
}

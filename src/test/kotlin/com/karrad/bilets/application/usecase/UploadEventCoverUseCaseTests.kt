package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.repository.CategoryRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.VenueRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@SpringJUnitConfig(ApplicationServicesTestConfig::class)
@Import(UploadEventCoverUseCase::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UploadEventCoverUseCaseTests {

    @Autowired lateinit var eventRepository: EventRepository
    @Autowired lateinit var venueRepository: VenueRepository
    @Autowired lateinit var organizationRepository: OrganizationRepository
    @Autowired lateinit var organizationMemberRepository: OrganizationMemberRepository
    @Autowired lateinit var categoryRepository: CategoryRepository
    @Autowired lateinit var useCase: UploadEventCoverUseCase

    private val orgId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")
    private val userId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002")
    private val eventId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000003")

    @AfterEach
    fun cleanup() {
        File("uploads/events/$eventId").deleteRecursively()
    }

    private fun setup() {
        organizationRepository.save(Organization(code = "test-org", name = "Test Org", id = orgId))
        organizationMemberRepository.save(
            OrganizationMember(organizationId = orgId, userId = userId, role = OrganizationMemberRole.OWNER)
        )
        val categoryId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001")
        categoryRepository.save(Category(code = "concert", label = "Concert", id = categoryId))
        val venueId = UUID.fromString("cccccccc-0000-0000-0000-000000000001")
        venueRepository.save(
            Venue(
                label = "Test Hall",
                city = City(label = "Moscow", subject = Subject(label = "Moscow")),
                organizationId = orgId,
                id = venueId
            )
        )
        eventRepository.save(
            Event(
                id = eventId,
                label = "Test Event",
                description = "desc",
                venueId = venueId,
                categoryId = categoryId,
                time = Instant.parse("2027-01-01T18:00:00Z"),
                ageRating = "18+",
                organizationId = orgId
            )
        )
    }

    private fun jpegFile(bytes: ByteArray = ByteArray(1024)) =
        MockMultipartFile("file", "cover.jpg", "image/jpeg", bytes)

    @Test
    fun `should upload cover successfully`() {
        setup()
        val result = useCase.upload(eventId, jpegFile(), userId)
        assertNotNull(result.imageUrl)
        assertContains(result.imageUrl!!, "events/$eventId/cover.jpg")
    }

    @Test
    fun `should reject empty file`() {
        setup()
        val emptyFile = MockMultipartFile("file", "cover.jpg", "image/jpeg", ByteArray(0))
        assertFailsWith<IllegalArgumentException> {
            useCase.upload(eventId, emptyFile, userId)
        }
    }

    @Test
    fun `should reject oversized file`() {
        setup()
        val bigFile = MockMultipartFile(
            "file", "cover.jpg", "image/jpeg",
            ByteArray((UploadEventCoverUseCase.MAX_FILE_SIZE + 1).toInt())
        )
        assertFailsWith<IllegalArgumentException> {
            useCase.upload(eventId, bigFile, userId)
        }
    }

    @Test
    fun `should reject invalid content type`() {
        setup()
        val pdfFile = MockMultipartFile("file", "doc.pdf", "application/pdf", ByteArray(1024))
        assertFailsWith<IllegalArgumentException> {
            useCase.upload(eventId, pdfFile, userId)
        }
    }

    @Test
    fun `should reject when event not found`() {
        setup()
        val unknownEventId = UUID.fromString("dddddddd-0000-0000-0000-000000000001")
        assertFailsWith<IllegalArgumentException> {
            useCase.upload(unknownEventId, jpegFile(), userId)
        }
    }

    @Test
    fun `should reject when user has no membership`() {
        setup()
        val strangerUserId = UUID.fromString("eeeeeeee-0000-0000-0000-000000000001")
        assertFailsWith<SecurityException> {
            useCase.upload(eventId, jpegFile(), strangerUserId)
        }
    }

    @Test
    fun `should reject when user organization does not own the event`() {
        setup()
        val otherOrgId = UUID.fromString("ffffffff-0000-0000-0000-000000000001")
        val otherUserId = UUID.fromString("ffffffff-0000-0000-0000-000000000002")
        organizationRepository.save(Organization(code = "other-org", name = "Other Org", id = otherOrgId))
        organizationMemberRepository.save(
            OrganizationMember(organizationId = otherOrgId, userId = otherUserId, role = OrganizationMemberRole.OWNER)
        )
        assertFailsWith<IllegalArgumentException> {
            useCase.upload(eventId, jpegFile(), otherUserId)
        }
    }
}

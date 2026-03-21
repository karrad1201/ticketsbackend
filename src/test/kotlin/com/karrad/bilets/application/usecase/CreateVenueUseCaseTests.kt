package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.VenueSpace
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
    lateinit var venueRepository: VenueRepository

    @Autowired
    lateinit var useCase: CreateVenueUseCase

    @Test
    fun `should create venue with spaces`() {
        val venue = demoVenue()

        val result = useCase.create(venue)

        assertEquals("Demo Hall", result.label)
        assertEquals(2, result.spaces.size)
        assertNotNull(venueRepository.findById(result.id))
    }

    @Test
    fun `should reject venue with duplicate space ids`() {
        val duplicateId = UUID.fromString("123e4567-e89b-12d3-a456-426614174601")

        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.create(
                Venue(
                    label = "Broken Hall",
                    city = City(
                        label = "Ekaterinburg",
                        subject = Subject(label = "Sverdlovsk Oblast")
                    ),
                    spaces = listOf(
                        VenueSpace(label = "Main Hall", id = duplicateId),
                        VenueSpace(label = "Small Hall", id = duplicateId)
                    )
                )
            )
        }

        assertTrue(exception.message!!.contains("unique"))
    }

    private fun demoVenue(): Venue {
        return Venue(
            label = "Demo Hall",
            city = City(label = "Ekaterinburg", subject = Subject(label = "Sverdlovsk Oblast")),
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

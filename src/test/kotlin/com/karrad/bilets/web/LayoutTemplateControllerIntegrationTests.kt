package com.karrad.bilets.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.VenueSpace
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.repository.LayoutTemplateRepository
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.domain.repository.VenueRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.util.UUID

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LayoutTemplateControllerIntegrationTests {

    lateinit var mockMvc: MockMvc

    @Autowired lateinit var authTokenRepository: AuthTokenRepository
    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var venueRepository: VenueRepository

    @Autowired
    lateinit var organizationRepository: OrganizationRepository

    @Autowired
    lateinit var organizationMemberRepository: OrganizationMemberRepository

    @Autowired
    lateinit var layoutTemplateRepository: LayoutTemplateRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Test
    fun `should create layout template over http`() {
        val venue = demoVenue()
        seedOrganizationAccess()
        venueRepository.save(venue)

        mockMvc.perform(
            post("/api/v1/layout-templates")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(demoCreatorUserId())}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "venueSpaceId" to venue.spaces.first().id,
                            "label" to "Theatre Layout",
                            "sections" to listOf(
                                mapOf(
                                    "label" to "Партер",
                                    "key" to "parter",
                                    "rows" to listOf(
                                        mapOf(
                                            "label" to "Ряд 1",
                                            "key" to "r1",
                                            "startSeat" to 1,
                                            "endSeat" to 3,
                                            "price" to 2000
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.label").value("Theatre Layout"))
            .andExpect(jsonPath("$.venueSpaceId").value(venue.spaces.first().id.toString()))
            .andExpect(jsonPath("$.sections.length()").value(1))
    }

    @Test
    fun `should reject layout template creation when venue space is missing`() {
        userRepository.save(demoCreator())
        mockMvc.perform(
            post("/api/v1/layout-templates")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(demoCreatorUserId())}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "venueSpaceId" to "123e4567-e89b-12d3-a456-426614174521",
                            "label" to "Theatre Layout",
                            "sections" to emptyList<Any>()
                        )
                    )
                )
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should reject layout template creation when creator is not organization member`() {
        organizationRepository.save(demoOrganization())
        venueRepository.save(demoVenue())
        userRepository.save(demoCreator())

        mockMvc.perform(
            post("/api/v1/layout-templates")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(demoCreatorUserId())}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "venueSpaceId" to demoVenue().spaces.first().id,
                            "label" to "Theatre Layout",
                            "sections" to emptyList<Any>()
                        )
                    )
                )
        )
            .andExpect(status().isBadRequest)
    }

    private fun seedOrganizationAccess() {
        organizationRepository.save(demoOrganization())
        userRepository.save(demoCreator())
        organizationMemberRepository.save(
            OrganizationMember(
                organizationId = demoOrganization().id,
                userId = demoCreatorUserId(),
                role = OrganizationMemberRole.OWNER
            )
        )
    }

    private fun demoOrganization(): Organization {
        return Organization(
            code = "demo-org",
            name = "Demo Org",
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174529")
        )
    }

    private fun demoCreatorUserId(): UUID =
        UUID.fromString("123e4567-e89b-12d3-a456-426614174532")

    private fun demoCreator(): User = User(
        email = "layout-creator@example.com",
        fullName = "Layout Creator",
        id = demoCreatorUserId()
    )

    private fun demoVenue(): Venue {
        return Venue(
            label = "Demo Hall",
            city = City(label = "Ekaterinburg", subject = Subject(label = "Sverdlovsk Oblast")),
            organizationId = demoOrganization().id,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174530"),
            spaces = listOf(
                VenueSpace(
                    label = "Main Hall",
                    id = UUID.fromString("123e4567-e89b-12d3-a456-426614174531")
                )
            )
        )
    }
}

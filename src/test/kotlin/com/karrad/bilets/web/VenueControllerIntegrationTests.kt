package com.karrad.bilets.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.AuthTokenRepository
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
import com.karrad.bilets.support.MockMvcSecurityHelper
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class VenueControllerIntegrationTests {

    lateinit var mockMvc: MockMvc

    @Autowired lateinit var authTokenRepository: AuthTokenRepository
    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var organizationRepository: OrganizationRepository

    @Autowired
    lateinit var organizationMemberRepository: OrganizationMemberRepository

    @Autowired
    lateinit var venueRepository: VenueRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        val builder = MockMvcBuilders.webAppContextSetup(webApplicationContext)

        mockMvc = MockMvcSecurityHelper.withSpringSecurity(builder).build()
    }

    @Test
    fun `should create venue over http`() {
        seedOrganizationAccess()

        mockMvc.perform(
            post("/api/v1/venues")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(demoCreatorUserId())}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "label" to "Demo Hall",
                            "city" to mapOf(
                                "label" to "Ekaterinburg",
                                "subject" to mapOf("label" to "Sverdlovsk Oblast")
                            ),
                            "organizationId" to demoOrganization().id,
                            "spaces" to listOf(
                                mapOf("label" to "Main Hall"),
                                mapOf("label" to "Small Hall")
                            )
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.label").value("Demo Hall"))
            .andExpect(jsonPath("$.city.label").value("Ekaterinburg"))
            .andExpect(jsonPath("$.organizationId").value(demoOrganization().id.toString()))
            .andExpect(jsonPath("$.spaces.length()").value(2))
    }

    @Test
    fun `should reject venue over http when space ids are duplicated`() {
        seedOrganizationAccess()

        mockMvc.perform(
            post("/api/v1/venues")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(demoCreatorUserId())}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "label" to "Broken Hall",
                            "city" to mapOf(
                                "label" to "Ekaterinburg",
                                "subject" to mapOf("label" to "Sverdlovsk Oblast")
                            ),
                            "organizationId" to demoOrganization().id,
                            "spaces" to listOf(
                                mapOf(
                                    "label" to "Main Hall",
                                    "id" to "123e4567-e89b-12d3-a456-426614174621"
                                ),
                                mapOf(
                                    "label" to "Small Hall",
                                    "id" to "123e4567-e89b-12d3-a456-426614174621"
                                )
                            )
                        )
                    )
                )
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should reject venue over http when creator is not organization member`() {
        organizationRepository.save(demoOrganization())
        userRepository.save(demoCreator())

        mockMvc.perform(
            post("/api/v1/venues")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(demoCreatorUserId())}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "label" to "Demo Hall",
                            "city" to mapOf(
                                "label" to "Ekaterinburg",
                                "subject" to mapOf("label" to "Sverdlovsk Oblast")
                            ),
                            "organizationId" to demoOrganization().id,
                            "spaces" to listOf(mapOf("label" to "Main Hall"))
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
            id = java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174629")
        )
    }

    private fun demoCreatorUserId(): java.util.UUID =
        java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174630")

    private fun demoCreator(): User = User(
        email = "venue-creator@example.com",
        fullName = "Venue Creator",
        id = demoCreatorUserId()
    )
}

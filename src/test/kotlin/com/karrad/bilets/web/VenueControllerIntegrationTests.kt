package com.karrad.bilets.web

import com.fasterxml.jackson.databind.ObjectMapper
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

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class VenueControllerIntegrationTests {

    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var venueRepository: VenueRepository

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Test
    fun `should create venue over http`() {
        mockMvc.perform(
            post("/api/venues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "label" to "Demo Hall",
                            "city" to mapOf(
                                "label" to "Ekaterinburg",
                                "subject" to mapOf("label" to "Sverdlovsk Oblast")
                            ),
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
            .andExpect(jsonPath("$.spaces.length()").value(2))
    }

    @Test
    fun `should reject venue over http when space ids are duplicated`() {
        mockMvc.perform(
            post("/api/venues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        mapOf(
                            "label" to "Broken Hall",
                            "city" to mapOf(
                                "label" to "Ekaterinburg",
                                "subject" to mapOf("label" to "Sverdlovsk Oblast")
                            ),
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
}

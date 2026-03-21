package com.karrad.bilets.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.repository.OrganizationRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.util.UUID

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrganizationControllerIntegrationTests {

    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var organizationRepository: OrganizationRepository

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Test
    fun `should create and read organizations over http`() {
        mockMvc.perform(
            post("/api/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("code" to "ufa-jazz", "name" to "Ufa Jazz Collective")))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value("ufa-jazz"))

        val organization = organizationRepository.findByCode("ufa-jazz")!!

        mockMvc.perform(get("/api/organizations"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))

        mockMvc.perform(get("/api/organizations/${organization.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Ufa Jazz Collective"))
    }

    @Test
    fun `should reject duplicate organization code over http`() {
        organizationRepository.save(
            Organization(
                code = "ufa-jazz",
                name = "Ufa Jazz Collective",
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614175301")
            )
        )

        mockMvc.perform(
            post("/api/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("code" to "ufa-jazz", "name" to "Another Label")))
        )
            .andExpect(status().isBadRequest)
    }
}

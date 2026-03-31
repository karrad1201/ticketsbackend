package com.karrad.bilets.web

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest
class GeoControllerIntegrationTests {

    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Test
    fun `GET geo cities returns 200 with 23 cities`() {
        mockMvc.perform(get("/api/geo/cities"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(23))
    }

    @Test
    fun `each city has id, label, subject id and subject label`() {
        mockMvc.perform(get("/api/geo/cities"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").isNotEmpty)
            .andExpect(jsonPath("$[0].label").isNotEmpty)
            .andExpect(jsonPath("$[0].subject.id").isNotEmpty)
            .andExpect(jsonPath("$[0].subject.label").isNotEmpty)
    }

    @Test
    fun `Moskva is in the list with correct subject`() {
        mockMvc.perform(get("/api/geo/cities"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.id == 'b0000000-0000-0000-0000-000000000001')].label").value("Москва"))
            .andExpect(jsonPath("$[?(@.id == 'b0000000-0000-0000-0000-000000000001')].subject.id").value("a0000000-0000-0000-0000-000000000001"))
            .andExpect(jsonPath("$[?(@.id == 'b0000000-0000-0000-0000-000000000001')].subject.label").value("Москва"))
    }
}

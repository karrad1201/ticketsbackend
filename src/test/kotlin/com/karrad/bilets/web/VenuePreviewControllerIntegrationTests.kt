package com.karrad.bilets.web

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest
class VenuePreviewControllerIntegrationTests {

    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Test
    fun `root preview should return html with demo venue and render payload`() {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith("text/html"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("<title>Demo Hall</title>")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("const venueStruct =")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("const renderLayout =")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Город: Ekaterinburg")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Сцена")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Партер")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Балкон")))
    }
}

package com.karrad.bilets.web

import com.karrad.bilets.domain.entity.Category
import com.fasterxml.jackson.databind.ObjectMapper
import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.entity.LayoutTemplate
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationApplication
import com.karrad.bilets.domain.entity.Row
import com.karrad.bilets.domain.entity.Section
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.VenueSpace
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.CategoryRepository
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.LayoutTemplateRepository
import com.karrad.bilets.domain.repository.OrganizationApplicationRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.domain.repository.VenueRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.Instant
import java.util.UUID

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReadApiIntegrationTests {

    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var organizationApplicationRepository: OrganizationApplicationRepository

    @Autowired
    lateinit var organizationRepository: OrganizationRepository

    @Autowired
    lateinit var categoryRepository: CategoryRepository

    @Autowired
    lateinit var venueRepository: VenueRepository

    @Autowired
    lateinit var layoutTemplateRepository: LayoutTemplateRepository

    @Autowired
    lateinit var eventRepository: EventRepository

    @Autowired
    lateinit var eventInventoryPlanRepository: EventInventoryPlanRepository

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Test
    fun `should expose list and get endpoints for current domain entities`() {
        val category = Category(code = "theatre", label = "Theatre", id = UUID.fromString("123e4567-e89b-12d3-a456-426614175100"))
        val user = User(email = "user@example.com", fullName = "Regular User", role = UserRole.USER, id = UUID.fromString("123e4567-e89b-12d3-a456-426614175098"))
        val application = OrganizationApplication(
            applicantUserId = user.id,
            organizationCode = "ural-live",
            organizationName = "Ural Live Events",
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614175097")
        )
        val organization = Organization(code = "ufa-jazz", name = "Ufa Jazz Collective", id = UUID.fromString("123e4567-e89b-12d3-a456-426614175099"))
        val venue = demoVenue()
        val layoutTemplate = demoLayoutTemplate(venue.spaces.first().id)
        val event = demoEvent(category.id, venue.id, venue.spaces.first().id)
        val inventoryPlan = EventInventoryPlan.seated(event, layoutTemplate)

        userRepository.save(user)
        organizationApplicationRepository.save(application)
        organizationRepository.save(organization)
        categoryRepository.save(category)
        venueRepository.save(venue)
        layoutTemplateRepository.save(layoutTemplate)
        eventRepository.save(event)
        eventInventoryPlanRepository.save(inventoryPlan)

        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))

        mockMvc.perform(get("/api/users/${user.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("user@example.com"))

        mockMvc.perform(get("/api/organization-applications"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))

        mockMvc.perform(get("/api/organization-applications/${application.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.organizationCode").value("ural-live"))

        mockMvc.perform(get("/api/organizations"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))

        mockMvc.perform(get("/api/organizations/${organization.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("ufa-jazz"))

        mockMvc.perform(get("/api/categories"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))

        mockMvc.perform(get("/api/categories/${category.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("theatre"))

        mockMvc.perform(get("/api/venues"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))

        mockMvc.perform(get("/api/venues/${venue.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.label").value("Demo Hall"))

        mockMvc.perform(get("/api/layout-templates"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))

        mockMvc.perform(get("/api/layout-templates/${layoutTemplate.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.label").value("Main Hall Layout"))

        mockMvc.perform(get("/api/events"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))

        mockMvc.perform(get("/api/events/${event.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.label").value("Hamlet"))

        mockMvc.perform(get("/api/inventory-plans"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))

        mockMvc.perform(get("/api/inventory-plans/${event.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.eventId").value(event.id.toString()))

        mockMvc.perform(get("/api/events/${event.id}/inventory"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.eventId").value(event.id.toString()))
    }

    @Test
    fun `should return 404 for missing resources on read endpoints`() {
        mockMvc.perform(get("/api/users/123e4567-e89b-12d3-a456-426614175109"))
            .andExpect(status().isNotFound)

        mockMvc.perform(get("/api/organization-applications/123e4567-e89b-12d3-a456-426614175108"))
            .andExpect(status().isNotFound)

        mockMvc.perform(get("/api/organizations/123e4567-e89b-12d3-a456-426614175110"))
            .andExpect(status().isNotFound)

        mockMvc.perform(get("/api/categories/123e4567-e89b-12d3-a456-426614175111"))
            .andExpect(status().isNotFound)

        mockMvc.perform(get("/api/venues/123e4567-e89b-12d3-a456-426614175112"))
            .andExpect(status().isNotFound)

        mockMvc.perform(get("/api/layout-templates/123e4567-e89b-12d3-a456-426614175113"))
            .andExpect(status().isNotFound)

        mockMvc.perform(get("/api/events/123e4567-e89b-12d3-a456-426614175114"))
            .andExpect(status().isNotFound)

        mockMvc.perform(get("/api/inventory-plans/123e4567-e89b-12d3-a456-426614175115"))
            .andExpect(status().isNotFound)

        mockMvc.perform(get("/api/events/123e4567-e89b-12d3-a456-426614175116/inventory"))
            .andExpect(status().isNotFound)
    }

    private fun demoVenue(): Venue {
        return Venue(
            label = "Demo Hall",
            city = City(label = "Ekaterinburg", subject = Subject(label = "Sverdlovsk Oblast")),
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614175101"),
            spaces = listOf(
                VenueSpace(
                    label = "Main Hall",
                    id = UUID.fromString("123e4567-e89b-12d3-a456-426614175102")
                )
            )
        )
    }

    private fun demoLayoutTemplate(venueSpaceId: UUID): LayoutTemplate {
        return LayoutTemplate(
            venueSpaceId = venueSpaceId,
            label = "Main Hall Layout",
            sections = listOf(
                Section(
                    label = "Партер",
                    key = "parter",
                    rows = listOf(Row(label = "Ряд 1", key = "r1", startSeat = 1, endSeat = 2, price = 2000))
                )
            ),
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614175103")
        )
    }

    private fun demoEvent(categoryId: UUID, venueId: UUID, venueSpaceId: UUID): Event {
        return Event(
            label = "Hamlet",
            description = "Evening show",
            venueId = venueId,
            categoryId = categoryId,
            time = Instant.parse("2026-04-01T18:00:00Z"),
            venueSpaceId = venueSpaceId,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614175104")
        )
    }
}

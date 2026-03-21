package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.repository.OrganizationRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringJUnitConfig(ApplicationServicesTestConfig::class)
@Import(CreateOrganizationUseCase::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CreateOrganizationUseCaseTests {

    @Autowired
    lateinit var organizationRepository: OrganizationRepository

    @Autowired
    lateinit var useCase: CreateOrganizationUseCase

    @Test
    fun `should create organization`() {
        val result = useCase.create(Organization(code = "ufa-jazz", name = "Ufa Jazz Collective"))

        assertEquals("ufa-jazz", result.code)
        assertNotNull(organizationRepository.findById(result.id))
    }

    @Test
    fun `should reject duplicate organization code`() {
        useCase.create(Organization(code = "ufa-jazz", name = "Ufa Jazz Collective"))

        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.create(Organization(code = "ufa-jazz", name = "Another Label"))
        }

        assertTrue(exception.message!!.contains("already exists"))
    }
}

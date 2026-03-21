package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.domain.repository.CategoryRepository
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
@Import(CreateCategoryUseCase::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CreateCategoryUseCaseTests {

    @Autowired
    lateinit var categoryRepository: CategoryRepository

    @Autowired
    lateinit var useCase: CreateCategoryUseCase

    @Test
    fun `should create category`() {
        val result = useCase.create(Category(code = "theatre", label = "Theatre"))

        assertEquals("theatre", result.code)
        assertNotNull(categoryRepository.findById(result.id))
    }

    @Test
    fun `should reject duplicate category code`() {
        useCase.create(Category(code = "theatre", label = "Theatre"))

        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.create(Category(code = "theatre", label = "Drama"))
        }

        assertTrue(exception.message!!.contains("already exists"))
    }
}

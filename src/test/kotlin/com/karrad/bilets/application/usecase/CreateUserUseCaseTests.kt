package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.repository.UserRepository
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
@Import(CreateUserUseCase::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CreateUserUseCaseTests {

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var useCase: CreateUserUseCase

    @Test
    fun `should create user`() {
        val result = useCase.create(User(email = "user@example.com", fullName = "Regular User"))

        assertEquals("user@example.com", result.email)
        assertNotNull(userRepository.findById(result.id))
    }

    @Test
    fun `should reject duplicate user email`() {
        useCase.create(User(email = "user@example.com", fullName = "Regular User"))

        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.create(User(email = "user@example.com", fullName = "Second User"))
        }

        assertTrue(exception.message!!.contains("already exists"))
    }
}

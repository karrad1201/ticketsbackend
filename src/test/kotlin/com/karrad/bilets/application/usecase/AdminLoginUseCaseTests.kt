package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.AdminCredential
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.AdminCredentialRepository
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@SpringJUnitConfig(ApplicationServicesTestConfig::class)
@Import(AdminLoginUseCase::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AdminLoginUseCaseTests {

    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var adminCredentialRepository: AdminCredentialRepository
    @Autowired lateinit var authTokenRepository: AuthTokenRepository
    @Autowired lateinit var passwordEncoder: PasswordEncoder
    @Autowired lateinit var useCase: AdminLoginUseCase

    private val phone = "+79001234567"

    private fun setupAdmin(password: String = "secret123"): User {
        val user = userRepository.save(User(fullName = "Admin", phone = phone, role = UserRole.ADMIN))
        adminCredentialRepository.save(
            AdminCredential(userId = user.id, passwordHash = passwordEncoder.encode(password)!!)
        )
        return user
    }

    @Test
    fun `should login admin with correct password`() {
        val admin = setupAdmin("secret123")

        val result = useCase.login(phone, "secret123")

        assertEquals(phone, result.user.phone)
        assertEquals(UserRole.ADMIN, result.user.role)
        assertNotNull(authTokenRepository.findByToken(result.accessToken))
    }

    @Test
    fun `should fail with wrong password`() {
        setupAdmin("secret123")

        assertFailsWith<IllegalArgumentException> {
            useCase.login(phone, "wrongpass")
        }
    }

    @Test
    fun `should fail when user not found`() {
        assertFailsWith<IllegalArgumentException> {
            useCase.login("+79999999999", "anypass")
        }
    }

    @Test
    fun `should fail for non-admin user`() {
        val user = userRepository.save(User(fullName = "Regular", phone = phone, role = UserRole.USER))
        adminCredentialRepository.save(
            AdminCredential(userId = user.id, passwordHash = passwordEncoder.encode("pass")!!)
        )

        assertFailsWith<IllegalArgumentException> {
            useCase.login(phone, "pass")
        }
    }

    @Test
    fun `should fail when admin has no credentials`() {
        userRepository.save(User(fullName = "Admin", phone = phone, role = UserRole.ADMIN))

        assertFailsWith<IllegalArgumentException> {
            useCase.login(phone, "anypass")
        }
    }

    @Test
    fun `should invalidate old token on repeated login`() {
        setupAdmin("pass")

        val first = useCase.login(phone, "pass")
        val second = useCase.login(phone, "pass")

        assertEquals(null, authTokenRepository.findByToken(first.accessToken), "Old token must be invalidated")
        assertNotNull(authTokenRepository.findByToken(second.accessToken))
    }
}

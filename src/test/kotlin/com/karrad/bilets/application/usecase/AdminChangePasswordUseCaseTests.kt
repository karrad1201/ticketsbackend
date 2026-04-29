package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.AdminCredential
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.AdminCredentialRepository
import com.karrad.bilets.domain.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@SpringJUnitConfig(ApplicationServicesTestConfig::class)
@Import(AdminChangePasswordUseCase::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AdminChangePasswordUseCaseTests {

    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var adminCredentialRepository: AdminCredentialRepository
    @Autowired lateinit var passwordEncoder: PasswordEncoder
    @Autowired lateinit var useCase: AdminChangePasswordUseCase

    private fun setupAdmin(password: String = "oldpass"): User {
        val user = userRepository.save(User(fullName = "Admin", phone = "+79001234567", role = UserRole.ADMIN))
        adminCredentialRepository.save(
            AdminCredential(userId = user.id, passwordHash = passwordEncoder.encode(password)!!)
        )
        return user
    }

    @Test
    fun `should change password successfully`() {
        val admin = setupAdmin("oldpass")

        useCase.changePassword(admin.id, "oldpass", "newpass1")

        val updated = adminCredentialRepository.findByUserId(admin.id)!!
        assertTrue(passwordEncoder.matches("newpass1", updated.passwordHash))
    }

    @Test
    fun `should fail with wrong current password`() {
        val admin = setupAdmin("oldpass")

        assertFailsWith<IllegalArgumentException> {
            useCase.changePassword(admin.id, "wrongpass", "newpass1")
        }
    }

    @Test
    fun `should fail if new password is too short`() {
        val admin = setupAdmin("oldpass")

        assertFailsWith<IllegalArgumentException> {
            useCase.changePassword(admin.id, "oldpass", "12345")
        }
    }

    @Test
    fun `should fail if no credentials found`() {
        val user = userRepository.save(User(fullName = "Admin", phone = "+79001234567", role = UserRole.ADMIN))

        assertFailsWith<IllegalStateException> {
            useCase.changePassword(user.id, "oldpass", "newpass1")
        }
    }
}

package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.AdminCredential
import com.karrad.bilets.domain.repository.AdminCredentialRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class AdminChangePasswordUseCase(
    private val adminCredentialRepository: AdminCredentialRepository,
    private val passwordEncoder: PasswordEncoder
) {
    fun changePassword(userId: UUID, currentPassword: String, newPassword: String) {
        require(newPassword.length >= 6) { "New password must be at least 6 characters" }

        val credential = adminCredentialRepository.findByUserId(userId)
            ?: throw IllegalStateException("Admin credentials not found")

        if (!passwordEncoder.matches(currentPassword, credential.passwordHash)) {
            throw IllegalArgumentException("Current password is incorrect")
        }

        val newHash: String = passwordEncoder.encode(newPassword)!!
        adminCredentialRepository.save(
            credential.copy(
                passwordHash = newHash,
                updatedAt = Instant.now()
            )
        )
    }
}

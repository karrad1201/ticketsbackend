package com.karrad.bilets.web

import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.UserRepository
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.UUID

@Component
class CurrentUserProvider(
    private val userRepository: UserRepository,
    private val authTokenRepository: AuthTokenRepository
) {
    fun requireUserId(): UUID = requireUser().id

    fun requireUser(): User {
        val request = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
            ?: throw IllegalStateException("No active HTTP request")

        // Primary: Bearer token (new auth)
        val authHeader = request.getHeader("Authorization")
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.removePrefix("Bearer ").trim()
            val authToken = authTokenRepository.findByToken(token)
                ?: throw IllegalArgumentException("Invalid or expired token")
            return requireNotNull(userRepository.findById(authToken.userId)) {
                "Authenticated user not found: ${authToken.userId}"
            }
        }

        // Fallback: X-User-Id header (legacy dev header)
        val userIdHeader = request.getHeader("X-User-Id")
            ?: throw IllegalArgumentException("Missing authorization: provide Bearer token or X-User-Id header")
        val userId = try {
            UUID.fromString(userIdHeader)
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid X-User-Id header: $userIdHeader")
        }
        return requireNotNull(userRepository.findById(userId)) { "Authenticated user not found: $userId" }
    }

    fun currentUserId(): UUID? = try { requireUserId() } catch (_: Exception) { null }

    fun requireAdmin(): User {
        val user = requireUser()
        require(user.role == UserRole.ADMIN) { "Authenticated user must be admin: ${user.id}" }
        return user
    }
}

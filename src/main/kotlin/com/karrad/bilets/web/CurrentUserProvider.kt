package com.karrad.bilets.web

import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.domain.security.BearerTokenRateLimiter
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.Clock
import java.util.UUID

@Component
class CurrentUserProvider(
    private val userRepository: UserRepository,
    private val authTokenRepository: AuthTokenRepository,
    private val clock: Clock,
    private val bearerTokenRateLimiter: BearerTokenRateLimiter
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
            if (authToken == null) {
                val ip = request.remoteAddr
                if (bearerTokenRateLimiter.recordFailure(ip)) {
                    throw TooManyRequestsException("Too many invalid token attempts")
                }
                throw UnauthorizedException("Invalid or expired token")
            }
            if (authToken.isExpired(clock.instant())) throw UnauthorizedException("Token has expired")
            return requireNotNull(userRepository.findById(authToken.userId)) {
                "Authenticated user not found: ${authToken.userId}"
            }
        }

        throw UnauthorizedException("Missing authorization: provide Bearer token")
    }

    fun currentUserId(): UUID? = try { requireUserId() } catch (_: Exception) { null }

    fun requireAdmin(): User {
        val user = requireUser()
        require(user.role == UserRole.ADMIN) { "Authenticated user must be admin: ${user.id}" }
        return user
    }
}

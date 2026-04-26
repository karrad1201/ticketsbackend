package com.karrad.bilets.web

import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.UserRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CurrentUserProvider(
    private val userRepository: UserRepository
) {
    /**
     * Возвращает UUID аутентифицированного пользователя.
     * Бросает UnauthorizedException если запрос не прошёл через BearerTokenAuthenticationFilter.
     */
    fun requireUserId(): UUID {
        return SecurityContextHolder.getContext().authentication
            ?.takeIf { it.isAuthenticated }
            ?.principal as? UUID
            ?: throw UnauthorizedException("Authentication required")
    }

    /**
     * Возвращает полный профиль аутентифицированного пользователя.
     */
    fun requireUser(): User {
        val userId = requireUserId()
        return userRepository.findById(userId)
            ?: throw UnauthorizedException("Authenticated user not found")
    }

    /**
     * Возвращает UUID текущего пользователя или null если запрос анонимный.
     * Используется на публичных эндпоинтах с опциональной персонализацией.
     */
    fun currentUserId(): UUID? {
        return SecurityContextHolder.getContext().authentication
            ?.takeIf { it.isAuthenticated }
            ?.principal as? UUID
    }

    /**
     * Требует роль ADMIN. Бросает SecurityException если роль другая.
     */
    fun requireAdmin(): User {
        val user = requireUser()
        if (user.role != UserRole.ADMIN) {
            throw ForbiddenException("Authenticated user must be admin: ${user.id}")
        }
        return user
    }
}

package com.karrad.bilets.web

import com.karrad.bilets.application.usecase.AdminChangePasswordUseCase
import com.karrad.bilets.application.usecase.AdminLoginUseCase
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.web.dto.AdminChangePasswordRequest
import com.karrad.bilets.web.dto.AdminLoginRequest
import com.karrad.bilets.web.dto.AuthResponse
import com.karrad.bilets.web.dto.UserResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Admin Auth", description = "Аутентификация администраторов по паролю")
@RestController
@RequestMapping("/admin/auth")
class AdminAuthController(
    private val adminLoginUseCase: AdminLoginUseCase,
    private val adminChangePasswordUseCase: AdminChangePasswordUseCase,
    private val currentUserProvider: CurrentUserProvider
) {

    @Operation(summary = "Вход администратора", description = "Аутентификация по номеру телефона и паролю (без SMS)")
    @PostMapping("/login")
    fun login(@RequestBody request: AdminLoginRequest): AuthResponse {
        val result = adminLoginUseCase.login(request.phone, request.password)
        return AuthResponse(
            accessToken = result.accessToken,
            refreshToken = result.refreshToken,
            user = result.user.toResponse()
        )
    }

    @Operation(summary = "Сменить пароль", description = "Смена пароля текущего администратора")
    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun changePassword(@RequestBody request: AdminChangePasswordRequest) {
        val user = currentUserProvider.requireAdmin()
        adminChangePasswordUseCase.changePassword(user.id, request.currentPassword, request.newPassword)
    }

    private fun User.toResponse() = UserResponse(
        id = id.toString(),
        fullName = fullName,
        phone = phone,
        email = email,
        role = role.name,
        avatarUrl = avatarUrl,
        interests = interests
    )
}

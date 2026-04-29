package com.karrad.bilets.web.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class SendCodeRequest(
    @field:NotBlank(message = "Phone must not be blank")
    @field:Pattern(regexp = "^\\+7\\d{10}$", message = "Phone must be in +7XXXXXXXXXX format")
    val phone: String
)

data class LoginRequest(
    @field:NotBlank(message = "Phone must not be blank")
    @field:Pattern(regexp = "^\\+7\\d{10}$", message = "Phone must be in +7XXXXXXXXXX format")
    val phone: String,
    @field:NotBlank(message = "Code must not be blank")
    @field:Pattern(regexp = "^\\d{6}$", message = "Code must be exactly 6 digits")
    val code: String
)

data class RegisterRequest(
    @field:NotBlank(message = "Phone must not be blank")
    @field:Pattern(regexp = "^\\+7\\d{10}$", message = "Phone must be in +7XXXXXXXXXX format")
    val phone: String,
    @field:NotBlank(message = "Code must not be blank")
    @field:Pattern(regexp = "^\\d{6}$", message = "Code must be exactly 6 digits")
    val code: String,
    @field:NotBlank(message = "Full name must not be blank")
    @field:Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    val fullName: String
)

data class RefreshRequest(
    @field:NotBlank(message = "Refresh token must not be blank")
    val refreshToken: String
)

data class AuthResponse(val accessToken: String, val refreshToken: String, val user: UserResponse)

data class UserResponse(
    val id: String,
    val fullName: String,
    val phone: String?,
    val email: String?,
    val role: String,
    val avatarUrl: String? = null,
    val interests: List<String> = emptyList()
)

package com.karrad.bilets.web.dto

data class SendCodeRequest(val phone: String)

data class LoginRequest(val phone: String, val code: String)

data class RegisterRequest(val phone: String, val code: String, val fullName: String)

data class AuthResponse(val token: String, val user: UserResponse)

data class UserResponse(
    val id: String,
    val fullName: String,
    val phone: String?,
    val email: String?,
    val role: String,
    val avatarUrl: String? = null,
    val interests: List<String> = emptyList()
)

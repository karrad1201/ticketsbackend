package com.karrad.bilets.web.dto

data class AdminLoginRequest(val phone: String, val password: String)

data class AdminChangePasswordRequest(val currentPassword: String, val newPassword: String)

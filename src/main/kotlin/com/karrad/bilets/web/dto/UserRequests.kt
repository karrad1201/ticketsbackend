package com.karrad.bilets.web.dto

import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.UserRole

data class CreateUserRequest(
    val email: String,
    val fullName: String,
    val role: UserRole = UserRole.USER
) {
    fun toDomain(): User = User(email = email, fullName = fullName, role = role)
}

package com.karrad.bilets.domain.entity

import com.karrad.bilets.domain.enums.UserRole
import java.util.UUID

data class User(
    val email: String,
    val fullName: String,
    val role: UserRole = UserRole.USER,
    val id: UUID = UUID.randomUUID()
) {
    init {
        require(email.isNotBlank()) { "User email must not be blank" }
        require(fullName.isNotBlank()) { "User fullName must not be blank" }
    }
}

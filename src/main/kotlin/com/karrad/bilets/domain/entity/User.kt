package com.karrad.bilets.domain.entity

import com.karrad.bilets.domain.enums.UserRole
import java.util.UUID

data class User(
    val fullName: String,
    val role: UserRole = UserRole.USER,
    val id: UUID = UUID.randomUUID(),
    val phone: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val interests: List<String> = emptyList()
) {
    init {
        require(fullName.isNotBlank()) { "User fullName must not be blank" }
        require(phone != null || email != null) { "User must have at least phone or email" }
        phone?.let { require(it.isNotBlank()) { "User phone must not be blank" } }
        email?.let { require(it.isNotBlank()) { "User email must not be blank" } }
    }
}

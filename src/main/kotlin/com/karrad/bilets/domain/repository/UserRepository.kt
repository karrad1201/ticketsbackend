package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.User
import java.util.UUID

interface UserRepository {
    fun save(user: User): User
    fun findById(id: UUID): User?
    fun findByEmail(email: String): User?
    fun findAll(): List<User>
    fun deleteById(id: UUID): Boolean
}

package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.repository.UserRepository
import java.util.UUID

class InMemoryUserRepository : UserRepository {
    private val storage = linkedMapOf<UUID, User>()

    override fun save(user: User): User {
        storage[user.id] = user
        return user
    }

    override fun findById(id: UUID): User? = storage[id]

    override fun findByEmail(email: String): User? = storage.values.firstOrNull { it.email == email }

    override fun findAll(): List<User> = storage.values.toList()

    override fun deleteById(id: UUID): Boolean = storage.remove(id) != null
}

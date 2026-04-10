package com.karrad.bilets.application.service

import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.repository.UserRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository
) {
    fun create(user: User): User = userRepository.save(user)

    fun getById(id: UUID): User? = userRepository.findById(id)

    fun list(): List<User> = userRepository.findAll()

    fun update(user: User): User {
        requireNotNull(userRepository.findById(user.id)) { "User not found: ${user.id}" }
        return userRepository.save(user)
    }

    fun deleteById(id: UUID): Boolean = userRepository.deleteById(id)

    fun updateProfile(userId: UUID, fullName: String?, interests: List<String>?): User {
        val user = requireNotNull(userRepository.findById(userId)) { "User not found: $userId" }
        return userRepository.save(
            user.copy(
                fullName = fullName?.trim()?.ifBlank { user.fullName } ?: user.fullName,
                interests = interests ?: user.interests
            )
        )
    }

    fun updateAvatar(userId: UUID, avatarUrl: String): User {
        val user = requireNotNull(userRepository.findById(userId)) { "User not found: $userId" }
        return userRepository.save(user.copy(avatarUrl = avatarUrl))
    }
}

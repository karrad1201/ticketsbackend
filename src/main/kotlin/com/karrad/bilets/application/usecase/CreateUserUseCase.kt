package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.repository.UserRepository
import org.springframework.stereotype.Component

@Component
class CreateUserUseCase(
    private val userRepository: UserRepository
) {
    fun create(user: User): User {
        require(userRepository.findByEmail(user.email) == null) {
            "User email already exists: ${user.email}"
        }
        return userRepository.save(user)
    }
}

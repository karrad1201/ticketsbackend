package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.repository.UserRepository
import org.springframework.stereotype.Component

@Component
class CreateUserUseCase(
    private val userRepository: UserRepository
) {
    fun create(user: User): User {
        user.email?.let { email ->
            require(userRepository.findByEmail(email) == null) {
                "User email already exists: $email"
            }
        }
        user.phone?.let { phone ->
            require(userRepository.findByPhone(phone) == null) {
                "User phone already exists: $phone"
            }
        }
        return userRepository.save(user)
    }
}

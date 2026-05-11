package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.PushToken
import com.karrad.bilets.domain.repository.PushTokenRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RegisterPushTokenUseCase(
    private val pushTokenRepository: PushTokenRepository
) {
    fun register(userId: UUID, token: String, platform: String): PushToken {
        require(platform in setOf("android", "ios")) { "Unknown platform: $platform" }
        return pushTokenRepository.save(
            PushToken(userId = userId, token = token, platform = platform)
        )
    }

    fun unregister(token: String) = pushTokenRepository.deleteByToken(token)
}

package com.karrad.bilets.application.service

import com.karrad.bilets.domain.push.PushMessage
import com.karrad.bilets.domain.push.PushNotificationGateway
import com.karrad.bilets.domain.repository.PushTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PushNotificationService(
    private val pushTokenRepository: PushTokenRepository,
    private val pushGateway: PushNotificationGateway
) {
    private val log = LoggerFactory.getLogger(PushNotificationService::class.java)

    fun sendToUser(userId: UUID, message: PushMessage) {
        val tokens = pushTokenRepository.findByUserId(userId)
        if (tokens.isEmpty()) return
        tokens.forEach { pt ->
            try {
                pushGateway.send(pt.token, pt.platform, message)
            } catch (e: Exception) {
                log.warn("Push failed for token {}…: {}", pt.token.take(12), e.message)
            }
        }
    }
}

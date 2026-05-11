package com.karrad.bilets.infrastructure.push

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import com.karrad.bilets.domain.push.PushMessage
import com.karrad.bilets.domain.push.PushNotificationGateway
import org.slf4j.LoggerFactory

class FcmPushNotificationGateway(
    private val fcm: FirebaseMessaging
) : PushNotificationGateway {

    private val log = LoggerFactory.getLogger(FcmPushNotificationGateway::class.java)

    override fun send(deviceToken: String, platform: String, message: PushMessage) {
        try {
            val msg = Message.builder()
                .setToken(deviceToken)
                .setNotification(
                    Notification.builder()
                        .setTitle(message.title)
                        .setBody(message.body)
                        .build()
                )
                .putAllData(message.data)
                .build()
            val messageId = fcm.send(msg)
            log.debug("FCM sent platform={} messageId={}", platform, messageId)
        } catch (e: Exception) {
            log.warn("FCM send failed platform={} token={}… error={}", platform, deviceToken.take(12), e.message)
        }
    }
}

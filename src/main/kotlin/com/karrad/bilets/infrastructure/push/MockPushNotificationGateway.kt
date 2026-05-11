package com.karrad.bilets.infrastructure.push

import com.karrad.bilets.domain.push.PushMessage
import com.karrad.bilets.domain.push.PushNotificationGateway
import org.slf4j.LoggerFactory

class MockPushNotificationGateway : PushNotificationGateway {
    private val log = LoggerFactory.getLogger(MockPushNotificationGateway::class.java)

    override fun send(deviceToken: String, platform: String, message: PushMessage) {
        log.info(
            "MOCK_PUSH platform={} token={}… title=\"{}\" body=\"{}\"",
            platform,
            deviceToken.take(12),
            message.title,
            message.body
        )
    }
}

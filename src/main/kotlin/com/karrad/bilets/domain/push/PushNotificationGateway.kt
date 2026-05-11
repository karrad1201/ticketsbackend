package com.karrad.bilets.domain.push

data class PushMessage(
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap()
)

interface PushNotificationGateway {
    /** Отправить push на конкретный токен устройства. Не бросает исключение при ошибке — просто логирует. */
    fun send(deviceToken: String, platform: String, message: PushMessage)
}

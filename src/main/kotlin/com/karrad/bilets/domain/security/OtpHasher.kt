package com.karrad.bilets.domain.security

import java.security.MessageDigest

/**
 * Утилита для хеширования одноразовых SMS-кодов перед сохранением в БД.
 *
 * Используется SHA-256. Для защиты от rainbow tables в качестве соли
 * применяется номер телефона, к которому привязан код.
 *
 * Формат: SHA-256(phone + ":" + code), hex-строка (64 символа).
 */
object OtpHasher {

    fun hash(phone: String, code: String): String {
        val input = "$phone:$code"
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

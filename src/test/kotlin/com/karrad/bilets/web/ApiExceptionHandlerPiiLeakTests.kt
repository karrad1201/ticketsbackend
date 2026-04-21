package com.karrad.bilets.web

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Проверяет, что error-ответы не содержат персональные данные пользователей (PII).
 *
 * Issue: ApiExceptionHandler возвращает exception.message as-is, что позволяет
 * перечислить существующих пользователей через атаку enumeration:
 *   "Phone already registered: +79991234567" — утечка телефона
 *   "No account found for phone +79991234567" — 404 раскрывает факт отсутствия аккаунта
 *
 * ОЖИДАЕМОЕ ПОВЕДЕНИЕ: detail не должен содержать конкретные PII (телефон, email).
 * ТЕКУЩЕЕ ПОВЕДЕНИЕ: detail == exception.message — тест УПАДЁТ до исправления.
 */
class ApiExceptionHandlerPiiLeakTests {

    private val handler = ApiExceptionHandler()

    @Test
    fun `400 response must not expose phone number from exception message`() {
        val ex = IllegalArgumentException("Phone already registered: +79991234567")
        val detail = handler.handleIllegalArgument(ex)

        assertFalse(
            detail.detail?.contains("+79991234567") ?: false,
            "Error detail must not expose phone number — enables enumeration attack"
        )
    }

    @Test
    fun `400 response must not expose email from exception message`() {
        val ex = IllegalArgumentException("User with email john@example.com already exists")
        val detail = handler.handleIllegalArgument(ex)

        assertFalse(
            detail.detail?.contains("john@example.com") ?: false,
            "Error detail must not expose email address"
        )
    }

    @Test
    fun `404 response must not expose phone from NoSuchElementException`() {
        val ex = NoSuchElementException("No account found for phone +79991234567. Please register first.")
        val detail = handler.handleNotFound(ex)

        assertFalse(
            detail.detail?.contains("+79991234567") ?: false,
            "404 detail must not expose whether phone is registered"
        )
    }

    @Test
    fun `409 response must not expose user internal state`() {
        val ex = IllegalStateException("Code already used for user id=3f4a1b2c-0000-0000-0000-000000000001")
        val detail = handler.handleIllegalState(ex)

        assertFalse(
            detail.detail?.contains("3f4a1b2c-0000-0000-0000-000000000001") ?: false,
            "Error detail must not expose internal user identifiers"
        )
    }

    @Test
    fun `response must still be informative without revealing PII`() {
        val ex = IllegalArgumentException("Phone already registered: +79991234567")
        val detail = handler.handleIllegalArgument(ex)

        // После исправления ожидается generic-сообщение, а не пустой ответ
        assertTrue(
            !detail.detail.isNullOrBlank(),
            "Error detail must not be empty after PII scrubbing"
        )
    }
}

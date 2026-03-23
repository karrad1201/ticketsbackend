package com.karrad.bilets.web

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

class ApiExceptionHandlerTests {

    private val handler = ApiExceptionHandler()

    @Test
    fun `should map illegal argument with explicit and fallback messages`() {
        val explicit = handler.handleIllegalArgument(IllegalArgumentException("bad input"))
        val fallback = handler.handleIllegalArgument(IllegalArgumentException())

        assertEquals(HttpStatus.BAD_REQUEST.value(), explicit.status)
        assertEquals("bad input", explicit.detail)
        assertEquals(HttpStatus.BAD_REQUEST.value(), fallback.status)
        assertEquals("Bad request", fallback.detail)
    }

    @Test
    fun `should map illegal state with explicit and fallback messages`() {
        val explicit = handler.handleIllegalState(IllegalStateException("conflict"))
        val fallback = handler.handleIllegalState(IllegalStateException())

        assertEquals(HttpStatus.CONFLICT.value(), explicit.status)
        assertEquals("conflict", explicit.detail)
        assertEquals(HttpStatus.CONFLICT.value(), fallback.status)
        assertEquals("Conflict", fallback.detail)
    }

    @Test
    fun `should map not found with explicit and fallback messages`() {
        val explicit = handler.handleNotFound(NoSuchElementException("missing"))
        val fallback = handler.handleNotFound(NoSuchElementException())

        assertEquals(HttpStatus.NOT_FOUND.value(), explicit.status)
        assertEquals("missing", explicit.detail)
        assertEquals(HttpStatus.NOT_FOUND.value(), fallback.status)
        assertEquals("Not found", fallback.detail)
    }
}

package com.karrad.bilets.domain.entity

import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Проверяет, что SeatKey.toString() не создаёт неоднозначный результат
 * при наличии двоеточий в составных частях ключа.
 *
 * Issue: SeatKey.init валидирует только isNotBlank(), но не запрещает двоеточие в значениях.
 * SeatKey.toString() = "$sectionKey:$rowKey:$seatKey" — если sectionKey содержит ":",
 * то split(":") по результату toString() даст неверное разбиение.
 *
 * Пример:
 *   SeatKey(sectionKey="VIP:A", rowKey="1", seatKey="5").toString() = "VIP:A:1:5"
 *   "VIP:A:1:5".split(":") = ["VIP", "A", "1", "5"] — 4 части вместо 3
 *   → парсер получит sectionKey="VIP", rowKey="A", seatKey="1" — НЕВЕРНО
 *
 * ОЖИДАЕМОЕ ПОВЕДЕНИЕ: SeatKey должен отклонять двоеточие в любой из частей.
 * ТЕКУЩЕЕ ПОВЕДЕНИЕ: принимает → toString() неоднозначен → тест УПАДЁТ до исправления.
 */
class SeatKeyAmbiguityTests {

    @Test
    fun `SeatKey with colon in sectionKey must be rejected`() {
        // "VIP:A" выглядит как иерархический ключ, но ломает toString()
        val ex = assertFailsWith<IllegalArgumentException> {
            SeatKey(sectionKey = "VIP:A", rowKey = "1", seatKey = "5")
        }
        assertTrue(
            ex.message?.contains("colon", ignoreCase = true) == true ||
            ex.message?.contains(":", ignoreCase = true) == true ||
            ex.message?.contains("section", ignoreCase = true) == true,
            "Error must mention colon restriction: ${ex.message}"
        )
    }

    @Test
    fun `SeatKey with colon in rowKey must be rejected`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            SeatKey(sectionKey = "A", rowKey = "1:2", seatKey = "5")
        }
        assertTrue(ex.message != null, "Exception must carry a message")
    }

    @Test
    fun `SeatKey with colon in seatKey must be rejected`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            SeatKey(sectionKey = "A", rowKey = "1", seatKey = "5:6")
        }
        assertTrue(ex.message != null, "Exception must carry a message")
    }

    @Test
    fun `SeatKey toString is unambiguously parseable for valid keys`() {
        // Нормальный случай — должен работать корректно
        val key = SeatKey(sectionKey = "VIP", rowKey = "12", seatKey = "345")
        val str = key.toString()
        val parts = str.split(":")
        assertEquals(3, parts.size, "toString() must produce exactly 3 colon-separated parts: '$str'")
        assertEquals("VIP", parts[0])
        assertEquals("12", parts[1])
        assertEquals("345", parts[2])
    }

    @Test
    fun `SeatKey toString produces exactly 3 colon-separated parts for valid keys`() {
        val cases = listOf(
            Triple("VIP", "A", "1"),
            Triple("PARQUET", "12", "999"),
            Triple("BALCONY", "ROW1", "50")
        )
        for ((section, row, seat) in cases) {
            val key = SeatKey(sectionKey = section, rowKey = row, seatKey = seat)
            val parts = key.toString().split(":")
            assertTrue(parts.size == 3, "toString() must produce 3 parts for ($section, $row, $seat): ${key}")
        }
    }
}

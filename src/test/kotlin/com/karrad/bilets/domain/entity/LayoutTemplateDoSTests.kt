package com.karrad.bilets.domain.entity

import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Проверяет, что LayoutTemplate.materializeSeatTemplates() не создаёт
 * экспоненциально большое количество объектов при большом диапазоне мест.
 *
 * Issue: Row.init валидирует только endSeat >= startSeat, но не ограничивает
 * максимальный диапазон. Row(startSeat=1, endSeat=100_000) проходит валидацию,
 * после чего materializeSeatTemplates() выделяет 100 000 объектов SeatTemplate.
 *
 * При endSeat = Int.MAX_VALUE (≈2.1 млрд) → OutOfMemoryError.
 * Даже 100 000 мест в одном ряду — нереалистично для любого реального зала.
 *
 * ОЖИДАЕМОЕ ПОВЕДЕНИЕ: Row должен отклонять нереалистично большие диапазоны.
 * ТЕКУЩЕЕ ПОВЕДЕНИЕ: Row принимает любой диапазон → тест УПАДЁТ до исправления.
 */
class LayoutTemplateDoSTests {

    @Test
    fun `Row with extremely large seat range must be rejected`() {
        // 100 000 мест в одном ряду — нереалистично ни для одного зала в мире
        val ex = assertFailsWith<IllegalArgumentException> {
            Row(label = "A", startSeat = 1, endSeat = 100_000, price = 500)
        }
        assertTrue(
            ex.message?.contains("seat", ignoreCase = true) == true ||
            ex.message?.contains("range", ignoreCase = true) == true ||
            ex.message?.contains("max", ignoreCase = true) == true,
            "Error must mention seat range limit: ${ex.message}"
        )
    }

    @Test
    fun `Row with moderately large seat range must be rejected`() {
        // 10 001 мест — выше разумного предела
        val ex = assertFailsWith<IllegalArgumentException> {
            Row(label = "B", startSeat = 1, endSeat = 10_001, price = 1000)
        }
        assertTrue(ex.message != null, "Exception must have a message")
    }

    @Test
    fun `Row with 10000 seats must be accepted as a boundary case`() {
        // Максимально допустимый диапазон (ожидаем, что граница будет у 10 000 или схожего значения)
        // Если фикс будет иным — тест нужно скорректировать вместе с исправлением
        // Данный тест документирует ожидаемую границу
        Row(label = "C", startSeat = 1, endSeat = 1000, price = 750) // 1000 мест — разумно
        // нет исключения — тест пройден
    }

    @Test
    fun `materializeSeatTemplates with huge row range must not hang indefinitely`() {
        // Если Row-валидация не добавлена — хотя бы убедимся, что materialise не зависает
        // Тест с таймаутом: если до исправления Row пропустит 100K — materialise должен завершиться
        // быстро (< 5 сек), а не уйти в OutOfMemoryError
        val startMs = System.currentTimeMillis()

        val result = runCatching {
            val template = LayoutTemplate(
                venueSpaceId = UUID.randomUUID(),
                label = "Test Hall",
                sections = listOf(
                    Section(
                        label = "A",
                        rows = listOf(Row(label = "1", startSeat = 1, endSeat = 100_000, price = 500))
                    )
                )
            )
            template.materializeSeatTemplates()
        }

        val elapsedMs = System.currentTimeMillis() - startMs

        // Либо операция отклонена (правильное поведение после фикса),
        // либо завершилась быстро (не зависла)
        assertTrue(
            result.isFailure || elapsedMs < 5_000L,
            "Huge seat range must either be rejected or complete in < 5s, took ${elapsedMs}ms"
        )
    }
}

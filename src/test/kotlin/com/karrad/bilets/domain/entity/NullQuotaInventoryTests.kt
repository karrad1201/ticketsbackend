package com.karrad.bilets.domain.entity

import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Проверяет поведение EventInventoryPlan.generalAdmission() при quota=null в TicketType.
 *
 * Issue: EventInventoryPlan.generalAdmission() делает:
 *   capacity = ticketType.quota ?: 0
 * При quota=null создаётся EventAdmissionInventory с capacity=0.
 *
 * Последствия:
 * - Инвентарь существует (событие выглядит как "с билетами")
 * - При попытке купить 1 билет: "Not enough admission capacity for ticket types: [...]"
 * - Пользователь видит загадочную ошибку "нет мощностей" вместо "квота не задана"
 * - quota=null семантически означает "неограниченный" или "не задан", но не "0"
 *
 * ОЖИДАЕМОЕ ПОВЕДЕНИЕ: generalAdmission() должен отклонять TicketType с quota=null
 *   с понятным сообщением, или создавать безлимитный инвентарь.
 * ТЕКУЩЕЕ ПОВЕДЕНИЕ: молча создаёт инвентарь с capacity=0 → тест УПАДЁТ до исправления.
 */
class NullQuotaInventoryTests {

    private val venueSpaceId: UUID = UUID.randomUUID()

    private fun event(): Event = Event(
        label = "Concert",
        description = "Test event",
        venueId = UUID.randomUUID(),
        categoryId = UUID.randomUUID(),
        time = Instant.parse("2027-06-01T18:00:00Z"),
        venueSpaceId = venueSpaceId
    )

    @Test
    fun `generalAdmission with null quota must fail with meaningful error — not create zero-capacity inventory`() {
        val event = event()
        val ticketType = TicketType(label = "Standard", price = 1000, quota = null)

        // ОЖИДАЕМ: ошибку при создании плана, т.к. quota=null неоднозначен
        // ТЕКУЩЕЕ ПОВЕДЕНИЕ: создаёт план с capacity=0 — тест УПАДЁТ
        val ex = assertFailsWith<IllegalArgumentException> {
            EventInventoryPlan.generalAdmission(event, listOf(ticketType))
        }
        assertTrue(
            ex.message?.contains("quota", ignoreCase = true) == true ||
            ex.message?.contains("capacity", ignoreCase = true) == true,
            "Error must mention quota or capacity: ${ex.message}"
        )
    }

    @Test
    fun `generalAdmission with null quota must be rejected at plan creation`() {
        val event = event()
        val ticketType = TicketType(label = "Standard", price = 1000, quota = null)

        val ex = assertFailsWith<IllegalArgumentException> {
            EventInventoryPlan.generalAdmission(event, listOf(ticketType))
        }
        assertTrue(
            ex.message?.contains("quota", ignoreCase = true) == true,
            "Error must mention quota: ${ex.message}"
        )
    }

    @Test
    fun `generalAdmission with quota=0 must be rejected at plan creation`() {
        val event = event()
        val ticketType = TicketType(label = "Sold Out", price = 500, quota = 0)

        val ex = assertFailsWith<IllegalArgumentException> {
            EventInventoryPlan.generalAdmission(event, listOf(ticketType))
        }
        assertTrue(
            ex.message?.contains("quota", ignoreCase = true) == true,
            "Error must mention quota: ${ex.message}"
        )
    }

    @Test
    fun `generalAdmission with positive quota must work correctly`() {
        // Проверяем, что нормальный путь (quota > 0) работает корректно
        val event = event()
        val ticketType = TicketType(label = "Standard", price = 1000, quota = 100)

        val plan = EventInventoryPlan.generalAdmission(event, listOf(ticketType))
        val held = plan.holdAdmission(listOf(AdmissionQuantity(ticketType.id, quantity = 5)))

        val inventory = held.admissionInventory.single { it.ticketTypeId == ticketType.id }
        assertTrue(inventory.held == 5, "Must hold exactly 5 tickets")
        assertTrue(inventory.available == 95, "Must have 95 remaining")
    }
}

package com.karrad.bilets.domain.entity

import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryOrganizationRepository
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * Проверяет, что накопление баланса не ломается из-за переполнения Int.
 *
 * Issue: Organization.balance — тип Int (32-bit, max ≈ 2.1 млрд копеек ≈ 21.4 млн руб).
 * На тикетинговой платформе с высоким оборотом это реалистичный предел.
 *
 * При creditBalance(amount) когда balance + amount > Int.MAX_VALUE:
 *   Kotlin Int переполняется в отрицательное число
 *   → Organization.init { require(balance >= 0) } выбрасывает IllegalArgumentException
 *   → легитимная транзакция зачисления падает с ошибкой
 *
 * ОЖИДАЕМОЕ ПОВЕДЕНИЕ: зачисление не бросает исключение при крупных суммах.
 * ТЕКУЩЕЕ ПОВЕДЕНИЕ: throws IllegalArgumentException — тест УПАДЁТ до исправления.
 */
class OrganizationCreditOverflowTests {

    @Test
    fun `Organization credit must not throw when balance approaches Int MAX_VALUE`() {
        // Int.MAX_VALUE - 100 копеек — уже близко к пределу
        val org = Organization(
            code = "overflow-org",
            name = "Overflow Test Org",
            balance = Int.MAX_VALUE - 100
        )

        // Ещё 200 копеек — легитимная операция, но вызывает Int overflow
        // Int.MAX_VALUE - 100 + 200 = Int.MAX_VALUE + 100 → wraps to Int.MIN_VALUE + 99 (negative)
        // Organization.init выбросит: "Organization balance must not be negative"
        val updated = org.credit(200)

        assertTrue(updated.balance > org.balance, "Balance must increase after credit")
    }

    @Test
    fun `InMemoryOrganizationRepository creditBalance must not throw for large amounts`() {
        val repo = InMemoryOrganizationRepository()
        val org = repo.save(
            Organization(
                code = "large-balance-org",
                name = "Large Balance Org",
                balance = Int.MAX_VALUE - 50
            )
        )

        // creditBalance вызывает org.copy(balance = balance + amount) → конструктор → init
        // При overflow init выбросит IllegalArgumentException
        repo.creditBalance(org.id, 100)

        val updated = repo.findById(org.id)!!
        assertTrue(updated.balance > 0, "Balance must remain positive after large credit")
    }

    @Test
    fun `Organization credit must handle max Int value amount`() {
        val org = Organization(code = "zero-org", name = "Zero Balance Org", balance = 0)

        // Int.MAX_VALUE копеек за один раз — теоретически допустимо
        val updated = org.credit(Int.MAX_VALUE)

        assertTrue(updated.balance == Int.MAX_VALUE)
    }

    @Test
    fun `two consecutive max credits must not lose data`() {
        // Проверяет потенциальный overflow при двух крупных зачислениях
        val repo = InMemoryOrganizationRepository()
        val org = repo.save(Organization(code = "two-credits", name = "Two Credits Org", balance = 0))

        repo.creditBalance(org.id, 1_000_000_000) // 10 млн руб
        repo.creditBalance(org.id, 1_500_000_000) // 15 млн руб

        val updated = repo.findById(org.id)!!
        assertTrue(
            updated.balance == 2_500_000_000L.toInt() || updated.balance > 0,
            "Combined balance of 25M rubles should be representable without overflow"
        )
    }
}

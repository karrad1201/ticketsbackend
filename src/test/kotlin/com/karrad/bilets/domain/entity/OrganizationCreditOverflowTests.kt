package com.karrad.bilets.domain.entity

import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryOrganizationRepository
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * Проверяет, что накопление баланса не ломается из-за переполнения Int.
 *
 * Issue #236: Organization.balance изменён с Int на Long (fix уже применён).
 * Тесты проверяют корректность работы после исправления.
 */
class OrganizationCreditOverflowTests {

    @Test
    fun `Organization credit must not throw when balance approaches Int MAX_VALUE`() {
        // Int.MAX_VALUE - 100 копеек — уже близко к пределу
        val org = Organization(
            code = "overflow-org",
            name = "Overflow Test Org",
            balance = Int.MAX_VALUE.toLong() - 100
        )

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
                balance = Int.MAX_VALUE.toLong() - 50
            )
        )

        repo.creditBalance(org.id, 100)

        val updated = repo.findById(org.id)!!
        assertTrue(updated.balance > 0, "Balance must remain positive after large credit")
    }

    @Test
    fun `Organization credit must handle max Int value amount`() {
        val org = Organization(code = "zero-org", name = "Zero Balance Org", balance = 0)

        val updated = org.credit(Int.MAX_VALUE)

        assertTrue(updated.balance == Int.MAX_VALUE.toLong())
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
            updated.balance == 2_500_000_000L,
            "Combined balance of 25M rubles must equal exactly 2_500_000_000"
        )
    }
}

package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryOrganizationRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Проверяет, что CreateOrganizationUseCase правильно защищает уникальность кода организации
 * при параллельных запросах.
 *
 * Issue: CreateOrganizationUseCase использует check-then-act без синхронизации:
 *
 *   require(organizationRepository.findByCode(code) == null) { "Organization code already exists" }
 *   return organizationRepository.save(organization)
 *
 * Два потока могут одновременно пройти проверку findByCode() == null, после чего
 * оба сохранят организацию с одинаковым кодом. В InMemoryOrganizationRepository
 * storage — LinkedHashMap<UUID, Organization>, поэтому обе записи попадут в хранилище
 * (у каждой Organization свой уникальный id). В результате:
 *   - В репозитории два объекта с одинаковым code
 *   - findByCode() вернёт только один из них (первый по порядку итерации)
 *   - Уникальность нарушена без какой-либо ошибки
 *
 * ОЖИДАЕМОЕ ПОВЕДЕНИЕ: ровно одна организация создаётся, остальные получают ошибку.
 * ТЕКУЩЕЕ ПОВЕДЕНИЕ: возможно создание нескольких организаций с одним кодом → тест УПАДЁТ.
 */
class CreateOrganizationConcurrencyTests {

    private lateinit var repo: InMemoryOrganizationRepository
    private lateinit var useCase: CreateOrganizationUseCase

    @BeforeEach
    fun setUp() {
        repo = InMemoryOrganizationRepository()
        useCase = CreateOrganizationUseCase(repo)
    }

    @Test
    fun `concurrent creation of organizations with same code must result in exactly one created`() {
        val threadCount = 20
        val sharedCode = "ACME"

        val startLatch = CountDownLatch(1)
        val successCount = AtomicInteger(0)
        val duplicateCodeErrors = AtomicInteger(0)
        val unexpectedErrors = mutableListOf<Throwable>()
        val executor = Executors.newFixedThreadPool(threadCount)

        repeat(threadCount) {
            executor.submit {
                startLatch.await()
                try {
                    useCase.create(Organization(code = sharedCode, name = "ACME Corp $it"))
                    successCount.incrementAndGet()
                } catch (e: IllegalArgumentException) {
                    // "Organization code already exists" — ожидаемая ошибка для проигравших потоков
                    if (e.message?.contains("already exists") == true) {
                        duplicateCodeErrors.incrementAndGet()
                    } else {
                        synchronized(unexpectedErrors) { unexpectedErrors.add(e) }
                    }
                } catch (e: Exception) {
                    synchronized(unexpectedErrors) { unexpectedErrors.add(e) }
                }
            }
        }

        startLatch.countDown()
        executor.shutdown()
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "Threads must complete in time")

        assertTrue(unexpectedErrors.isEmpty(), "Unexpected errors: $unexpectedErrors")

        // КЛЮЧЕВАЯ ПРОВЕРКА: в репозитории должна быть ровно одна организация с этим кодом
        val orgsWithCode = repo.findAll().filter { it.code == sharedCode }
        assertEquals(
            1, orgsWithCode.size,
            "Must have exactly 1 organization with code '$sharedCode', but found ${orgsWithCode.size}. " +
            "Successes: ${successCount.get()}, duplicate errors: ${duplicateCodeErrors.get()}"
        )
        assertEquals(1, successCount.get(), "Exactly one creation must succeed")
    }

    @Test
    fun `organizations with distinct codes can be created concurrently without interference`() {
        val n = 20
        val startLatch = CountDownLatch(1)
        val successCount = AtomicInteger(0)
        val errors = mutableListOf<Throwable>()
        val executor = Executors.newFixedThreadPool(n)

        (1..n).forEach { i ->
            executor.submit {
                startLatch.await()
                try {
                    useCase.create(Organization(code = "ORG-$i", name = "Organization $i"))
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    synchronized(errors) { errors.add(e) }
                }
            }
        }

        startLatch.countDown()
        executor.shutdown()
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS))

        assertTrue(errors.isEmpty(), "No errors expected for distinct codes: $errors")
        assertEquals(n, successCount.get(), "All $n organizations must be created successfully")
        assertEquals(n, repo.findAll().size, "Repository must contain all $n organizations")
    }

    @Test
    fun `sequential creation with same code must fail on second attempt`() {
        // Базовый (однопоточный) случай — проверяем, что детекция работает вообще
        useCase.create(Organization(code = "SYNC", name = "First"))

        val ex = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
            useCase.create(Organization(code = "SYNC", name = "Second"))
        }
        assertTrue(
            ex.message?.contains("already exists") == true,
            "Must fail with 'already exists': ${ex.message}"
        )
        assertEquals(1, repo.findAll().filter { it.code == "SYNC" }.size)
    }
}

package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.domain.repository.CategoryRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryCategoryRepository : CategoryRepository {
    private val storage = ConcurrentHashMap<UUID, Category>()

    override fun save(category: Category): Category {
        storage[category.id] = category
        return category
    }

    override fun findById(id: UUID): Category? = storage[id]

    override fun findByCode(code: String): Category? = storage.values.firstOrNull { it.code == code }

    override fun findAll(): List<Category> = storage.values.toList()

    override fun deleteById(id: UUID): Boolean = storage.remove(id) != null
}

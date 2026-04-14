package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.Category
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import java.util.UUID

interface CategoryRepository {
    @CacheEvict(cacheNames = ["categories.all"], allEntries = true)
    fun save(category: Category): Category
    fun findById(id: UUID): Category?
    fun findByCode(code: String): Category?
    @Cacheable("categories.all")
    fun findAll(): List<Category>
    @CacheEvict(cacheNames = ["categories.all"], allEntries = true)
    fun deleteById(id: UUID): Boolean
}

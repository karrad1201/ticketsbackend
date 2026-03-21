package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.Category
import java.util.UUID

interface CategoryRepository {
    fun save(category: Category): Category
    fun findById(id: UUID): Category?
    fun findByCode(code: String): Category?
    fun findAll(): List<Category>
    fun deleteById(id: UUID): Boolean
}

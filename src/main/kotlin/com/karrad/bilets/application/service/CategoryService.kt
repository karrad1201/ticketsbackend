package com.karrad.bilets.application.service

import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.domain.repository.CategoryRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository
) {
    fun create(category: Category): Category = categoryRepository.save(category)

    fun getById(id: UUID): Category? = categoryRepository.findById(id)

    fun list(): List<Category> = categoryRepository.findAll()

    fun update(category: Category): Category {
        requireNotNull(categoryRepository.findById(category.id)) { "Category not found: ${category.id}" }
        return categoryRepository.save(category)
    }

    fun deleteById(id: UUID): Boolean = categoryRepository.deleteById(id)
}

package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.domain.repository.CategoryRepository
import org.springframework.stereotype.Component

@Component
class CreateCategoryUseCase(
    private val categoryRepository: CategoryRepository
) {
    fun create(category: Category): Category {
        require(categoryRepository.findByCode(category.code) == null) {
            "Category code already exists: ${category.code}"
        }
        return categoryRepository.save(category)
    }
}

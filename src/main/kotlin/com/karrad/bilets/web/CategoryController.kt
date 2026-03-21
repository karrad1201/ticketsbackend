package com.karrad.bilets.web

import com.karrad.bilets.application.service.CategoryService
import com.karrad.bilets.application.usecase.CreateCategoryUseCase
import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.web.dto.CreateCategoryRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/categories")
class CategoryController(
    private val createCategoryUseCase: CreateCategoryUseCase,
    private val categoryService: CategoryService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateCategoryRequest): Category {
        return createCategoryUseCase.create(request.toDomain())
    }

    @GetMapping
    fun list(): List<Category> = categoryService.list()

    @GetMapping("/{categoryId}")
    fun getById(@PathVariable categoryId: UUID): Category =
        categoryService.getById(categoryId) ?: throw NoSuchElementException("Category not found: $categoryId")
}

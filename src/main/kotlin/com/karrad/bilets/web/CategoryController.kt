package com.karrad.bilets.web

import com.karrad.bilets.application.service.CategoryService
import com.karrad.bilets.application.usecase.CreateCategoryUseCase
import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.web.dto.CreateCategoryRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "Categories", description = "Управление категориями мероприятий")
@RestController
@RequestMapping("/api/v1/categories")
class CategoryController(
    private val createCategoryUseCase: CreateCategoryUseCase,
    private val categoryService: CategoryService
) {

    @Operation(summary = "Создать категорию", description = "Добавляет новую категорию мероприятий")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Категория создана"),
        ApiResponse(responseCode = "400", description = "Некорректные данные"),
        ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
        ApiResponse(responseCode = "403", description = "Требуются права администратора")
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateCategoryRequest): Category {
        return createCategoryUseCase.create(request.toDomain())
    }

    @Operation(summary = "Список категорий", description = "Возвращает все доступные категории мероприятий")
    @ApiResponse(responseCode = "200", description = "Список категорий")
    @GetMapping
    fun list(): List<Category> = categoryService.list()

    @Operation(summary = "Получить категорию по ID", description = "Возвращает категорию по её идентификатору")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Категория найдена"),
        ApiResponse(responseCode = "404", description = "Категория не найдена")
    )
    @GetMapping("/{categoryId}")
    fun getById(
        @Parameter(description = "Идентификатор категории") @PathVariable categoryId: UUID
    ): Category =
        categoryService.getById(categoryId) ?: throw NoSuchElementException("Category not found: $categoryId")
}

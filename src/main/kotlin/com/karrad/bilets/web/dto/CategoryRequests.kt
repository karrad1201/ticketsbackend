package com.karrad.bilets.web.dto

import com.karrad.bilets.domain.entity.Category

data class CreateCategoryRequest(
    val code: String,
    val label: String
) {
    fun toDomain(): Category = Category(code = code, label = label)
}

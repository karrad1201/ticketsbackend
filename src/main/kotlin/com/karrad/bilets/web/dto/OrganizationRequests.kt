package com.karrad.bilets.web.dto

import com.karrad.bilets.domain.entity.Organization

data class CreateOrganizationRequest(
    val code: String,
    val name: String
) {
    fun toDomain(): Organization = Organization(code = code, name = name)
}

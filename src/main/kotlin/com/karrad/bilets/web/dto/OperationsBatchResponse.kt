package com.karrad.bilets.web.dto

import java.util.UUID

data class OperationsBatchResponse(
    val processedCount: Int,
    val ids: List<UUID>
)

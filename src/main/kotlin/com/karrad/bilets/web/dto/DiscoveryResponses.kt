package com.karrad.bilets.web.dto

import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.domain.entity.Event

data class DiscoveryFeedResponse(
    val forYou: List<Event>,
    val byCategory: List<CategoryEventsEntry>,
    val tomorrow: List<Event>,
    val dayAfterTomorrow: List<Event>
)

data class CategoryEventsEntry(
    val category: Category,
    val events: List<Event>
)

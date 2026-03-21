package com.karrad.bilets.web.dto

import com.karrad.bilets.domain.entity.Event

data class EventDiscoveryResponse(
    val seenOrganizations: List<Event>,
    val favoriteCategories: List<Event>,
    val tomorrow: List<Event>,
    val dayAfterTomorrow: List<Event>
)

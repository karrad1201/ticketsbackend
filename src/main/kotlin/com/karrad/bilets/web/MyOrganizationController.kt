package com.karrad.bilets.web

import com.karrad.bilets.application.usecase.GetMyOrganizationEventsUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/v1/my/organization")
class MyOrganizationController(
    private val getMyOrganizationEventsUseCase: GetMyOrganizationEventsUseCase,
    private val currentUserProvider: CurrentUserProvider
) {
    @GetMapping("/events")
    fun myEvents(): List<MyOrganizationEventItem> {
        val callerId = currentUserProvider.requireUserId()
        return getMyOrganizationEventsUseCase.execute(callerId).map {
            MyOrganizationEventItem(id = it.id, label = it.label, time = it.time)
        }
    }
}

data class MyOrganizationEventItem(
    val id: UUID,
    val label: String,
    val time: Instant
)

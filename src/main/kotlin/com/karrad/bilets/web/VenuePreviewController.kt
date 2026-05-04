package com.karrad.bilets.web

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RootController {

    @GetMapping("/")
    fun root(): Map<String, String> = mapOf("status" to "ok", "service" to "tickets-api")
}

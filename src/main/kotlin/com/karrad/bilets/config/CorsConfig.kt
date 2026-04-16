package com.karrad.bilets.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CorsConfig(
    @Value("\${app.cors.allowed-origins:*}") private val allowedOrigins: String
) : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns(allowedOrigins)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(allowedOrigins != "*")
            .maxAge(3600)
        registry.addMapping("/auth/**")
            .allowedOriginPatterns(allowedOrigins)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(allowedOrigins != "*")
            .maxAge(3600)
    }
}

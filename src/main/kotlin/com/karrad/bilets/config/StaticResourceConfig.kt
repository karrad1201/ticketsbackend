package com.karrad.bilets.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class StaticResourceConfig(
    @Value("\${app.uploads.dir}") private val uploadsDir: String
) : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val location = if (uploadsDir.startsWith("/")) "file:$uploadsDir/" else "file:$uploadsDir/"
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations(location)
    }
}

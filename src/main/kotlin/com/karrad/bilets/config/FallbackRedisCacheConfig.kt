package com.karrad.bilets.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.CacheManager
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = true)
class FallbackRedisCacheConfig {

    @Bean("redisCacheManager")
    fun redisCacheManagerFallback(): CacheManager =
        ConcurrentMapCacheManager("events", "eventLists", "eventSearch", "discovery", "favorites", "authTokens", "users")
}

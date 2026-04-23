package com.karrad.bilets.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import java.time.Duration

@Configuration
@ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "jdbc")
class RedisCacheConfig(private val objectMapper: ObjectMapper) {

    @Bean("redisCacheManager")
    fun redisCacheManager(connectionFactory: RedisConnectionFactory): CacheManager {
        val redisMapper = objectMapper.copy().activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder().allowIfSubType(Any::class.java).build(),
            ObjectMapper.DefaultTyping.NON_FINAL
        )
        val valueSerializer = GenericJackson2JsonRedisSerializer(redisMapper)
        val valuePair = RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer)
        val base = RedisCacheConfiguration.defaultCacheConfig()
            .serializeValuesWith(valuePair)
            .disableCachingNullValues()

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(base)
            .withInitialCacheConfigurations(
                mapOf(
                    "events" to base.entryTtl(Duration.ofMinutes(10)),
                    "eventLists" to base.entryTtl(Duration.ofSeconds(30)),
                    "eventSearch" to base.entryTtl(Duration.ofSeconds(30)),
                    "discovery" to base.entryTtl(Duration.ofSeconds(30)),
                    "favorites" to base.entryTtl(Duration.ofMinutes(5)),
                    "authTokens" to base.entryTtl(Duration.ofMinutes(5)),
                    "users" to base.entryTtl(Duration.ofMinutes(5))
                )
            )
            .build()
    }
}

package com.counseling.api.config

import io.lettuce.core.api.StatefulConnection
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration
import org.springframework.data.redis.core.ReactiveStringRedisTemplate

@Configuration
@Profile("!test")
class RedisConfig(
    private val redisProperties: DataRedisProperties,
) {
    @Bean
    fun lettuceConnectionFactory(): LettuceConnectionFactory {
        val standaloneConfig =
            RedisStandaloneConfiguration(
                redisProperties.host,
                redisProperties.port,
            )
        if (!redisProperties.password.isNullOrEmpty()) {
            standaloneConfig.setPassword(redisProperties.password)
        }

        val pool = redisProperties.lettuce.pool
        val poolConfig =
            GenericObjectPoolConfig<StatefulConnection<*, *>>().apply {
                maxTotal = pool.maxActive
                maxIdle = pool.maxIdle
                minIdle = pool.minIdle
            }
        val clientConfig =
            LettucePoolingClientConfiguration
                .builder()
                .poolConfig(poolConfig)
                .build()
        return LettuceConnectionFactory(standaloneConfig, clientConfig)
    }

    @Bean
    fun reactiveStringRedisTemplate(connectionFactory: ReactiveRedisConnectionFactory): ReactiveStringRedisTemplate =
        ReactiveStringRedisTemplate(connectionFactory)
}

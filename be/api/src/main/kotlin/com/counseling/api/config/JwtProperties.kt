package com.counseling.api.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val secret: String,
    val accessExpiration: Long = 3_600_000L,
    val refreshExpiration: Long = 604_800_000L,
)

package com.counseling.api.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "livekit")
data class LiveKitProperties(
    val url: String,
    val apiKey: String,
    val apiSecret: String,
    val tokenTtlSeconds: Long = 3600L,
)

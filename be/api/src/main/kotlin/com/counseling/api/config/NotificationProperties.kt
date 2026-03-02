package com.counseling.api.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "notification")
data class NotificationProperties(
    val mailFrom: String = "noreply@counseling.com",
)

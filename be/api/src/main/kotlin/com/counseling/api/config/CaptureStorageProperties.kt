package com.counseling.api.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "capture-storage")
data class CaptureStorageProperties(
    val basePath: String = "/tmp/screen-captures",
    val maxFileSize: Long = 10_485_760,
)

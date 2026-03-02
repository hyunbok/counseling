package com.counseling.api.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "recording")
data class RecordingProperties(
    val basePath: String = "/tmp/recordings",
    val fileFormat: String = "mp4",
)

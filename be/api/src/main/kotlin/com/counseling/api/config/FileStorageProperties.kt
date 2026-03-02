package com.counseling.api.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "file-storage")
data class FileStorageProperties(
    val basePath: String = "/tmp/shared-files",
    val maxFileSize: Long = 10_485_760,
    val allowedTypes: List<String> =
        listOf(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain",
        ),
)

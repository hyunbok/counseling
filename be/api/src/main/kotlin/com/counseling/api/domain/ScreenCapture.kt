package com.counseling.api.domain

import java.time.Instant

data class ScreenCapture(
    val id: String? = null,
    val channelId: String,
    val capturedBy: String,
    val originalFilename: String,
    val storedFilename: String,
    val contentType: String,
    val fileSize: Long,
    val storagePath: String,
    val note: String?,
    val createdAt: Instant,
    val deleted: Boolean = false,
)

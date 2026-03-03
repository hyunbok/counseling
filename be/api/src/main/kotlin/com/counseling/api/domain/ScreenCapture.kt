package com.counseling.api.domain

import java.time.Instant
import java.util.UUID

data class ScreenCapture(
    val id: UUID,
    val channelId: UUID,
    val capturedBy: UUID,
    val originalFilename: String,
    val storedFilename: String,
    val contentType: String,
    val fileSize: Long,
    val storagePath: String,
    val note: String?,
    val createdAt: Instant,
    val deleted: Boolean = false,
)

package com.counseling.api.domain

import java.time.Instant
import java.util.UUID

data class SharedFile(
    val id: UUID,
    val channelId: UUID,
    val uploaderId: String,
    val uploaderType: SenderType,
    val originalFilename: String,
    val storedFilename: String,
    val contentType: String,
    val fileSize: Long,
    val storagePath: String,
    val createdAt: Instant,
    val deleted: Boolean = false,
)

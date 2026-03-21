package com.counseling.api.domain

import java.time.Instant

data class SharedFile(
    val id: String? = null,
    val channelId: String,
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

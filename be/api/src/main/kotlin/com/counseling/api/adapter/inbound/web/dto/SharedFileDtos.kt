package com.counseling.api.adapter.inbound.web.dto

import java.time.Instant

data class SharedFileResponse(
    val id: String?,
    val channelId: String,
    val uploaderId: String,
    val uploaderType: String,
    val originalFilename: String,
    val contentType: String,
    val fileSize: Long,
    val createdAt: Instant,
)

data class SharedFileListResponse(
    val files: List<SharedFileResponse>,
    val hasMore: Boolean,
    val oldestTimestamp: Instant?,
)

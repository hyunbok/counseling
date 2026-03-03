package com.counseling.api.adapter.inbound.web.dto

import java.time.Instant
import java.util.UUID

data class SharedFileResponse(
    val id: UUID,
    val channelId: UUID,
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

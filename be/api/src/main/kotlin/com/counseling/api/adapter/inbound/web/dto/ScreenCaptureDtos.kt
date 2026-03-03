package com.counseling.api.adapter.inbound.web.dto

import java.time.Instant
import java.util.UUID

data class ScreenCaptureResponse(
    val id: UUID,
    val channelId: UUID,
    val capturedBy: UUID,
    val originalFilename: String,
    val contentType: String,
    val fileSize: Long,
    val note: String?,
    val createdAt: Instant,
)

data class ScreenCaptureListResponse(
    val captures: List<ScreenCaptureResponse>,
    val hasMore: Boolean,
    val oldestTimestamp: Instant?,
)

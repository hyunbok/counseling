package com.counseling.api.adapter.inbound.web.dto

import java.time.Instant

data class ScreenCaptureResponse(
    val id: String?,
    val channelId: String,
    val capturedBy: String,
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

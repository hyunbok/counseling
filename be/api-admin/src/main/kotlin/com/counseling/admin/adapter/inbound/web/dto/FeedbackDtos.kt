package com.counseling.admin.adapter.inbound.web.dto

import java.time.Instant
import java.util.UUID

data class FeedbackResponse(
    val id: UUID,
    val channelId: UUID,
    val rating: Int,
    val comment: String?,
    val createdAt: Instant,
)

data class FeedbackDetailResponse(
    val id: UUID,
    val channelId: UUID,
    val rating: Int,
    val comment: String?,
    val createdAt: Instant,
)

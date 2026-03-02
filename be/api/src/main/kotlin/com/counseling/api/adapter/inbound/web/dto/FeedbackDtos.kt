package com.counseling.api.adapter.inbound.web.dto

import java.time.Instant
import java.util.UUID

data class SubmitFeedbackRequest(
    val rating: Int,
    val comment: String?,
)

data class FeedbackResponse(
    val id: UUID,
    val channelId: UUID,
    val rating: Int,
    val comment: String?,
    val createdAt: Instant,
)

data class FeedbackStatsResponse(
    val totalCount: Long,
    val averageRating: Double,
    val distribution: Map<Int, Long>,
    val from: Instant,
    val to: Instant,
)

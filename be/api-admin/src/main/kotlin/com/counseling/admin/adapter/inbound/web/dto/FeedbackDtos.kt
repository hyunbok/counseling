package com.counseling.admin.adapter.inbound.web.dto

import java.time.Instant

data class FeedbackResponse(
    val id: String?,
    val channelId: String,
    val rating: Int,
    val comment: String?,
    val createdAt: Instant,
)

data class FeedbackDetailResponse(
    val id: String?,
    val channelId: String,
    val rating: Int,
    val comment: String?,
    val createdAt: Instant,
)

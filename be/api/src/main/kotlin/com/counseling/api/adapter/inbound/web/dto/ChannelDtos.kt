package com.counseling.api.adapter.inbound.web.dto

import java.time.Instant

data class ChannelTokenResponse(
    val token: String,
    val roomName: String,
    val identity: String,
    val livekitUrl: String,
)

data class ChannelDetailResponse(
    val id: String?,
    val agentId: String?,
    val status: String,
    val livekitRoomName: String?,
    val customerName: String?,
    val customerContact: String?,
    val startedAt: Instant?,
    val endedAt: Instant?,
    val createdAt: Instant,
)

data class ChannelSummaryResponse(
    val id: String?,
    val status: String,
    val customerName: String?,
    val startedAt: Instant?,
    val endedAt: Instant?,
    val createdAt: Instant,
)

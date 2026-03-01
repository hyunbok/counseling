package com.counseling.api.adapter.inbound.web.dto

import java.time.Instant
import java.util.UUID

data class ChannelTokenResponse(
    val token: String,
    val roomName: String,
    val identity: String,
    val livekitUrl: String,
)

data class ChannelDetailResponse(
    val id: UUID,
    val agentId: UUID?,
    val status: String,
    val livekitRoomName: String?,
    val customerName: String?,
    val customerContact: String?,
    val startedAt: Instant?,
    val endedAt: Instant?,
    val createdAt: Instant,
)

data class ChannelSummaryResponse(
    val id: UUID,
    val status: String,
    val customerName: String?,
    val startedAt: Instant?,
    val endedAt: Instant?,
    val createdAt: Instant,
)

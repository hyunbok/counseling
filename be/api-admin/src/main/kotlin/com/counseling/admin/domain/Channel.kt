package com.counseling.admin.domain

import java.time.Instant
import java.util.UUID

data class Channel(
    val id: UUID,
    val agentId: UUID?,
    val status: ChannelStatus,
    val startedAt: Instant?,
    val endedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

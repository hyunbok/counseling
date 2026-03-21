package com.counseling.admin.domain

import java.time.Instant

data class Channel(
    val id: String? = null,
    val agentId: String?,
    val status: ChannelStatus,
    val startedAt: Instant?,
    val endedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

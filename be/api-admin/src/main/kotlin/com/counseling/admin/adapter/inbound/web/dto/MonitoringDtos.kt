package com.counseling.admin.adapter.inbound.web.dto

import java.time.Instant
import java.util.UUID

data class ActiveChannelResponse(
    val id: UUID,
    val agentId: UUID?,
    val status: String,
    val startedAt: Instant?,
    val createdAt: Instant,
)

data class AgentStatusResponse(
    val agentId: String,
    val agentName: String,
    val status: String,
    val active: Boolean,
)

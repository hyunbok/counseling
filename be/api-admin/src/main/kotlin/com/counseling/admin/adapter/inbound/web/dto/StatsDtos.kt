package com.counseling.admin.adapter.inbound.web.dto

import java.util.UUID

data class StatsSummaryResponse(
    val totalChannels: Long,
    val completedChannels: Long,
    val averageRating: Double,
    val averageHandleTimeSeconds: Long,
)

data class AgentStatsResponse(
    val agentId: UUID,
    val agentName: String,
    val totalChannels: Long,
    val completedChannels: Long,
    val averageRating: Double,
    val averageHandleTimeSeconds: Long,
)

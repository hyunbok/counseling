package com.counseling.admin.port.inbound

import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

data class StatsSummary(
    val totalChannels: Long,
    val completedChannels: Long,
    val averageRating: Double,
    val averageHandleTimeSeconds: Long,
)

data class AgentStats(
    val agentId: UUID,
    val agentName: String,
    val totalChannels: Long,
    val completedChannels: Long,
    val averageRating: Double,
    val averageHandleTimeSeconds: Long,
)

interface StatsQuery {
    fun getSummary(
        from: Instant,
        to: Instant,
    ): Mono<StatsSummary>

    fun getAgentStats(
        from: Instant,
        to: Instant,
    ): Mono<List<AgentStats>>
}

package com.counseling.admin.port.outbound

import com.counseling.admin.port.inbound.AgentStats
import com.counseling.admin.port.inbound.StatsSummary
import reactor.core.publisher.Mono
import java.time.Instant

interface AdminStatsRepository {
    fun getSummary(
        from: Instant,
        to: Instant,
    ): Mono<StatsSummary>

    fun getAgentStats(
        from: Instant,
        to: Instant,
    ): Mono<List<AgentStats>>
}

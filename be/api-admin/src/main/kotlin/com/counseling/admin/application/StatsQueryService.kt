package com.counseling.admin.application

import com.counseling.admin.port.inbound.AgentStats
import com.counseling.admin.port.inbound.StatsQuery
import com.counseling.admin.port.inbound.StatsSummary
import com.counseling.admin.port.outbound.AdminStatsRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Instant

@Service
@Profile("!test")
class StatsQueryService(
    private val statsRepository: AdminStatsRepository,
) : StatsQuery {
    override fun getSummary(
        from: Instant,
        to: Instant,
    ): Mono<StatsSummary> = statsRepository.getSummary(from, to)

    override fun getAgentStats(
        from: Instant,
        to: Instant,
    ): Mono<List<AgentStats>> = statsRepository.getAgentStats(from, to)
}

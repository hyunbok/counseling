package com.counseling.admin.adapter.inbound.web.controller

import com.counseling.admin.adapter.inbound.web.dto.AgentStatsResponse
import com.counseling.admin.adapter.inbound.web.dto.StatsSummaryResponse
import com.counseling.admin.port.inbound.StatsQuery
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.time.Instant

@RestController
@RequestMapping("/api-adm/stats")
@Profile("!test")
class StatsController(
    private val statsQuery: StatsQuery,
) {
    @GetMapping("/summary")
    fun getSummary(
        @RequestParam from: Instant,
        @RequestParam to: Instant,
    ): Mono<StatsSummaryResponse> =
        statsQuery.getSummary(from, to).map { summary ->
            StatsSummaryResponse(
                totalChannels = summary.totalChannels,
                completedChannels = summary.completedChannels,
                averageRating = summary.averageRating,
                averageHandleTimeSeconds = summary.averageHandleTimeSeconds,
            )
        }

    @GetMapping("/agents")
    fun getAgentStats(
        @RequestParam from: Instant,
        @RequestParam to: Instant,
    ): Mono<List<AgentStatsResponse>> =
        statsQuery.getAgentStats(from, to).map { statsList ->
            statsList.map { stats ->
                AgentStatsResponse(
                    agentId = stats.agentId,
                    agentName = stats.agentName,
                    totalChannels = stats.totalChannels,
                    completedChannels = stats.completedChannels,
                    averageRating = stats.averageRating,
                    averageHandleTimeSeconds = stats.averageHandleTimeSeconds,
                )
            }
        }
}

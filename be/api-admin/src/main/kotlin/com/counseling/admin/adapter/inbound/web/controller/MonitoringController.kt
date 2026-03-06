package com.counseling.admin.adapter.inbound.web.controller

import com.counseling.admin.adapter.inbound.web.dto.ActiveChannelResponse
import com.counseling.admin.adapter.inbound.web.dto.AgentStatusResponse
import com.counseling.admin.port.inbound.MonitoringQuery
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api-adm/monitoring")
@Profile("!test")
class MonitoringController(
    private val monitoringQuery: MonitoringQuery,
) {
    @GetMapping("/channels")
    fun getActiveChannels(): Flux<ActiveChannelResponse> =
        monitoringQuery.getActiveChannels().map { channel ->
            ActiveChannelResponse(
                id = channel.id,
                agentId = channel.agentId,
                status = channel.status.name,
                startedAt = channel.startedAt,
                createdAt = channel.createdAt,
            )
        }

    @GetMapping("/agents")
    fun getAgentStatuses(
        @RequestParam(required = false) status: String?,
    ): Mono<List<AgentStatusResponse>> =
        monitoringQuery.getAgentStatuses(status).map { statuses ->
            statuses.map { info ->
                AgentStatusResponse(
                    agentId = info.agentId,
                    agentName = info.agentName,
                    status = info.status,
                    active = info.active,
                )
            }
        }
}

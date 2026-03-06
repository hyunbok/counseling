package com.counseling.admin.port.inbound

import com.counseling.admin.domain.Channel
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

data class AgentStatusInfo(
    val agentId: String,
    val agentName: String,
    val status: String,
    val active: Boolean,
)

interface MonitoringQuery {
    fun getActiveChannels(): Flux<Channel>

    fun getAgentStatuses(status: String? = null): Mono<List<AgentStatusInfo>>
}

package com.counseling.admin.application

import com.counseling.admin.domain.Channel
import com.counseling.admin.port.inbound.AgentStatusInfo
import com.counseling.admin.port.inbound.MonitoringQuery
import com.counseling.admin.port.outbound.AdminAgentRepository
import com.counseling.admin.port.outbound.AdminChannelRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
@Profile("!test")
class MonitoringQueryService(
    private val channelRepository: AdminChannelRepository,
    private val agentRepository: AdminAgentRepository,
) : MonitoringQuery {
    override fun getActiveChannels(): Flux<Channel> = channelRepository.findAllActiveChannels()

    override fun getAgentStatuses(): Mono<List<AgentStatusInfo>> =
        agentRepository
            .findAllByNotDeleted()
            .map { agent ->
                AgentStatusInfo(
                    agentId = agent.id.toString(),
                    agentName = agent.name,
                    status = agent.agentStatus.name,
                    active = agent.active,
                )
            }.collectList()
}

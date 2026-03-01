package com.counseling.api.port.outbound

import com.counseling.api.domain.Channel
import com.counseling.api.domain.ChannelStatus
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface ChannelRepository {
    fun save(channel: Channel): Mono<Channel>

    fun findByIdAndNotDeleted(id: UUID): Mono<Channel>

    fun findAllByAgentIdAndNotDeleted(agentId: UUID): Flux<Channel>

    fun findAllByStatusAndNotDeleted(status: ChannelStatus): Flux<Channel>

    fun findAllByAgentIdAndStatusAndNotDeleted(
        agentId: UUID,
        status: ChannelStatus,
    ): Flux<Channel>
}

package com.counseling.api.port.outbound

import com.counseling.api.domain.Channel
import com.counseling.api.domain.ChannelStatus
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface ChannelRepository {
    fun save(channel: Channel): Mono<Channel>

    fun findByIdAndNotDeleted(id: String): Mono<Channel>

    fun findAllByAgentIdAndNotDeleted(agentId: String): Flux<Channel>

    fun findAllByStatusAndNotDeleted(status: ChannelStatus): Flux<Channel>

    fun findAllByAgentIdAndStatusAndNotDeleted(
        agentId: String,
        status: ChannelStatus,
    ): Flux<Channel>
}

package com.counseling.api.port.inbound

import com.counseling.api.domain.Channel
import com.counseling.api.domain.ChannelStatus
import com.counseling.api.domain.Endpoint
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

data class TokenResult(
    val token: String,
    val roomName: String,
    val identity: String,
    val livekitUrl: String,
)

data class ChannelDetail(
    val channel: Channel,
    val endpoints: List<Endpoint>,
)

interface ChannelUseCase {
    fun getAgentToken(
        channelId: UUID,
        agentId: UUID,
    ): Mono<TokenResult>

    fun getCustomerToken(
        channelId: UUID,
        customerName: String,
    ): Mono<TokenResult>

    fun closeChannel(
        channelId: UUID,
        agentId: UUID,
    ): Mono<Void>

    fun getChannel(channelId: UUID): Mono<ChannelDetail>

    fun getAgentChannels(
        agentId: UUID,
        status: ChannelStatus?,
    ): Flux<Channel>
}

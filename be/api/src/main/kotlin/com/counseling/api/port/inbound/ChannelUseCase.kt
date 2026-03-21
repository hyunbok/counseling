package com.counseling.api.port.inbound

import com.counseling.api.domain.Channel
import com.counseling.api.domain.ChannelStatus
import com.counseling.api.domain.Endpoint
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

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
        channelId: String,
        agentId: String,
    ): Mono<TokenResult>

    fun getCustomerToken(
        channelId: String,
        customerName: String,
    ): Mono<TokenResult>

    fun closeChannel(
        channelId: String,
        agentId: String,
    ): Mono<Void>

    fun getChannel(channelId: String): Mono<ChannelDetail>

    fun getAgentChannels(
        agentId: String,
        status: ChannelStatus?,
    ): Flux<Channel>
}

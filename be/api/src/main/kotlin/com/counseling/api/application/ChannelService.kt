package com.counseling.api.application

import com.counseling.api.config.LiveKitProperties
import com.counseling.api.domain.AgentStatus
import com.counseling.api.domain.Channel
import com.counseling.api.domain.ChannelStatus
import com.counseling.api.domain.EndpointType
import com.counseling.api.domain.exception.ConflictException
import com.counseling.api.domain.exception.NotFoundException
import com.counseling.api.port.inbound.ChannelDetail
import com.counseling.api.port.inbound.ChannelUseCase
import com.counseling.api.port.inbound.TokenResult
import com.counseling.api.port.outbound.AgentRepository
import com.counseling.api.port.outbound.ChannelRepository
import com.counseling.api.port.outbound.EndpointRepository
import com.counseling.api.port.outbound.LiveKitPort
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Service
@Profile("!test")
class ChannelService(
    private val channelRepository: ChannelRepository,
    private val endpointRepository: EndpointRepository,
    private val agentRepository: AgentRepository,
    private val liveKitPort: LiveKitPort,
    private val liveKitProperties: LiveKitProperties,
) : ChannelUseCase {
    override fun getAgentToken(
        channelId: UUID,
        agentId: UUID,
    ): Mono<TokenResult> =
        channelRepository
            .findByIdAndNotDeleted(channelId)
            .switchIfEmpty(Mono.error(NotFoundException("Channel not found: $channelId")))
            .flatMap { channel ->
                if (!channel.isOpen()) {
                    return@flatMap Mono.error(ConflictException("Channel is not open: $channelId"))
                }
                if (channel.agentId != agentId) {
                    return@flatMap Mono.error(ConflictException("Agent $agentId is not owner of channel $channelId"))
                }
                val roomName =
                    channel.livekitRoomName
                        ?: return@flatMap Mono.error(ConflictException("Channel has no LiveKit room: $channelId"))
                agentRepository
                    .findByIdAndNotDeleted(agentId)
                    .switchIfEmpty(Mono.error(NotFoundException("Agent not found: $agentId")))
                    .map { agent ->
                        val identity = "agent:$agentId"
                        val token = liveKitPort.generateToken(roomName, identity, agent.name)
                        TokenResult(
                            token = token,
                            roomName = roomName,
                            identity = identity,
                            livekitUrl = liveKitProperties.url,
                        )
                    }
            }

    override fun getCustomerToken(
        channelId: UUID,
        customerName: String,
    ): Mono<TokenResult> =
        channelRepository
            .findByIdAndNotDeleted(channelId)
            .switchIfEmpty(Mono.error(NotFoundException("Channel not found: $channelId")))
            .flatMap { channel ->
                if (!channel.isOpen()) {
                    return@flatMap Mono.error(ConflictException("Channel is not open: $channelId"))
                }
                val roomName =
                    channel.livekitRoomName
                        ?: return@flatMap Mono.error(ConflictException("Channel has no LiveKit room: $channelId"))
                endpointRepository
                    .findAllByChannelId(channelId)
                    .filter { it.type == EndpointType.CUSTOMER && it.customerName == customerName }
                    .next()
                    .switchIfEmpty(
                        Mono.error(NotFoundException("Customer endpoint not found for name: $customerName")),
                    ).map { _ ->
                        val identity = "customer:$customerName"
                        val token = liveKitPort.generateToken(roomName, identity, customerName)
                        TokenResult(
                            token = token,
                            roomName = roomName,
                            identity = identity,
                            livekitUrl = liveKitProperties.url,
                        )
                    }
            }

    override fun closeChannel(
        channelId: UUID,
        agentId: UUID,
    ): Mono<Void> =
        channelRepository
            .findByIdAndNotDeleted(channelId)
            .switchIfEmpty(Mono.error(NotFoundException("Channel not found: $channelId")))
            .flatMap { channel ->
                if (!channel.isOpen()) {
                    return@flatMap Mono.error(ConflictException("Channel is not open: $channelId"))
                }
                if (channel.agentId != agentId) {
                    return@flatMap Mono.error(ConflictException("Agent $agentId is not owner of channel $channelId"))
                }
                val roomName = channel.livekitRoomName
                val deleteRoom =
                    if (roomName != null) {
                        liveKitPort.deleteRoom(roomName)
                    } else {
                        Mono.empty()
                    }
                deleteRoom
                    .then(
                        endpointRepository
                            .findAllByChannelId(channelId)
                            .flatMap { endpoint -> endpointRepository.save(endpoint.leave()) }
                            .then(),
                    ).then(channelRepository.save(channel.close()))
                    .then(
                        agentRepository
                            .findByIdAndNotDeleted(agentId)
                            .flatMap { agent -> agentRepository.save(agent.updateStatus(AgentStatus.ONLINE)) },
                    ).then()
            }

    override fun getChannel(channelId: UUID): Mono<ChannelDetail> =
        channelRepository
            .findByIdAndNotDeleted(channelId)
            .switchIfEmpty(Mono.error(NotFoundException("Channel not found: $channelId")))
            .flatMap { channel ->
                endpointRepository
                    .findAllByChannelId(channelId)
                    .collectList()
                    .map { endpoints -> ChannelDetail(channel = channel, endpoints = endpoints) }
            }

    override fun getAgentChannels(
        agentId: UUID,
        status: ChannelStatus?,
    ): Flux<Channel> =
        if (status != null) {
            channelRepository.findAllByAgentIdAndStatusAndNotDeleted(agentId, status)
        } else {
            channelRepository.findAllByAgentIdAndNotDeleted(agentId)
        }
}

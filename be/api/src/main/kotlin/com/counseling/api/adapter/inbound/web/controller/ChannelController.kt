package com.counseling.api.adapter.inbound.web.controller

import com.counseling.api.adapter.inbound.web.dto.ChannelDetailResponse
import com.counseling.api.adapter.inbound.web.dto.ChannelSummaryResponse
import com.counseling.api.adapter.inbound.web.dto.ChannelTokenResponse
import com.counseling.api.domain.ChannelStatus
import com.counseling.api.domain.EndpointType
import com.counseling.api.domain.auth.AuthenticatedAgent
import com.counseling.api.domain.exception.UnauthorizedException
import com.counseling.api.port.inbound.ChannelUseCase
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping("/api/channels")
@Profile("!test")
class ChannelController(
    private val channelUseCase: ChannelUseCase,
) {
    @GetMapping("/{channelId}/token")
    fun getAgentToken(
        @PathVariable channelId: UUID,
    ): Mono<ChannelTokenResponse> =
        authenticatedAgent().flatMap { principal ->
            channelUseCase
                .getAgentToken(channelId, principal.agentId)
                .map { result ->
                    ChannelTokenResponse(
                        token = result.token,
                        roomName = result.roomName,
                        identity = result.identity,
                        livekitUrl = result.livekitUrl,
                    )
                }
        }

    @GetMapping("/{channelId}/customer-token")
    fun getCustomerToken(
        @PathVariable channelId: UUID,
        @RequestParam name: String,
    ): Mono<ChannelTokenResponse> =
        channelUseCase
            .getCustomerToken(channelId, name)
            .map { result ->
                ChannelTokenResponse(
                    token = result.token,
                    roomName = result.roomName,
                    identity = result.identity,
                    livekitUrl = result.livekitUrl,
                )
            }

    @PostMapping("/{channelId}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun closeChannel(
        @PathVariable channelId: UUID,
    ): Mono<Void> =
        authenticatedAgent().flatMap { principal ->
            channelUseCase.closeChannel(channelId, principal.agentId)
        }

    @GetMapping("/{channelId}")
    fun getChannel(
        @PathVariable channelId: UUID,
    ): Mono<ChannelDetailResponse> =
        channelUseCase
            .getChannel(channelId)
            .map { detail ->
                val customerEndpoint = detail.endpoints.firstOrNull { it.type == EndpointType.CUSTOMER }
                ChannelDetailResponse(
                    id = detail.channel.id,
                    agentId = detail.channel.agentId,
                    status = detail.channel.status.name,
                    livekitRoomName = detail.channel.livekitRoomName,
                    customerName = customerEndpoint?.customerName,
                    customerContact = customerEndpoint?.customerContact,
                    startedAt = detail.channel.startedAt,
                    endedAt = detail.channel.endedAt,
                    createdAt = detail.channel.createdAt,
                )
            }

    @GetMapping
    fun getAgentChannels(
        @RequestParam(required = false) status: ChannelStatus?,
    ): Flux<ChannelSummaryResponse> =
        authenticatedAgent().flatMapMany { principal ->
            channelUseCase
                .getAgentChannels(principal.agentId, status)
                .map { channel ->
                    ChannelSummaryResponse(
                        id = channel.id,
                        status = channel.status.name,
                        customerName = null,
                        startedAt = channel.startedAt,
                        endedAt = channel.endedAt,
                        createdAt = channel.createdAt,
                    )
                }
        }

    private fun authenticatedAgent(): Mono<AuthenticatedAgent> =
        ReactiveSecurityContextHolder
            .getContext()
            .flatMap { ctx ->
                val principal = ctx.authentication?.principal
                if (principal is AuthenticatedAgent) {
                    Mono.just(principal)
                } else {
                    Mono.error(UnauthorizedException("Not authenticated"))
                }
            }
}

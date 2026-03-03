package com.counseling.api.adapter.inbound.web.controller

import com.counseling.api.adapter.inbound.web.dto.CoBrowsingSessionListResponse
import com.counseling.api.adapter.inbound.web.dto.CoBrowsingSessionResponse
import com.counseling.api.adapter.inbound.web.dto.toResponse
import com.counseling.api.domain.auth.AuthenticatedAgent
import com.counseling.api.domain.exception.UnauthorizedException
import com.counseling.api.port.inbound.CoBrowsingQuery
import com.counseling.api.port.inbound.CoBrowsingUseCase
import com.counseling.api.port.inbound.EndCoBrowsingCommand
import com.counseling.api.port.inbound.RequestCoBrowsingCommand
import com.counseling.api.port.inbound.StartCoBrowsingCommand
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
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
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/channels/{channelId}/co-browsing")
@Profile("!test")
class CoBrowsingController(
    private val coBrowsingUseCase: CoBrowsingUseCase,
    private val coBrowsingQuery: CoBrowsingQuery,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun requestSession(
        @PathVariable channelId: UUID,
    ): Mono<CoBrowsingSessionResponse> =
        authenticatedAgent().flatMap { agent ->
            coBrowsingUseCase
                .requestSession(RequestCoBrowsingCommand(channelId = channelId, agentId = agent.agentId))
                .map { it.toResponse() }
        }

    @PostMapping("/{sessionId}/start")
    fun startSession(
        @PathVariable channelId: UUID,
        @PathVariable sessionId: UUID,
    ): Mono<CoBrowsingSessionResponse> =
        coBrowsingUseCase
            .startSession(StartCoBrowsingCommand(channelId = channelId, sessionId = sessionId))
            .map { it.toResponse() }

    @PostMapping("/{sessionId}/end")
    fun endSession(
        @PathVariable channelId: UUID,
        @PathVariable sessionId: UUID,
    ): Mono<CoBrowsingSessionResponse> =
        coBrowsingUseCase
            .endSession(EndCoBrowsingCommand(channelId = channelId, sessionId = sessionId))
            .map { it.toResponse() }

    @GetMapping("/active")
    fun getActiveSession(
        @PathVariable channelId: UUID,
    ): Mono<CoBrowsingSessionResponse> =
        coBrowsingQuery
            .getActiveSession(channelId)
            .map { it.toResponse() }

    @GetMapping
    fun listSessions(
        @PathVariable channelId: UUID,
        @RequestParam(required = false) before: Instant?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): Mono<CoBrowsingSessionListResponse> =
        authenticatedAgent().flatMap {
            coBrowsingQuery
                .listSessions(channelId, before, limit.coerceIn(1, 100))
                .map { result ->
                    CoBrowsingSessionListResponse(
                        sessions = result.sessions.map { session -> session.toResponse() },
                        hasMore = result.hasMore,
                        oldestTimestamp = result.oldestTimestamp,
                    )
                }
        }

    @GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamUpdates(
        @PathVariable channelId: UUID,
    ): Flux<CoBrowsingSessionResponse> = coBrowsingQuery.streamUpdates(channelId).map { it.toResponse() }

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

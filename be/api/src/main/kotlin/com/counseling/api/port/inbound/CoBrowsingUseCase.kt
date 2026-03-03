package com.counseling.api.port.inbound

import com.counseling.api.domain.CoBrowsingSession
import reactor.core.publisher.Mono
import java.util.UUID

data class RequestCoBrowsingCommand(
    val channelId: UUID,
    val agentId: UUID,
)

data class StartCoBrowsingCommand(
    val channelId: UUID,
    val sessionId: UUID,
)

data class EndCoBrowsingCommand(
    val channelId: UUID,
    val sessionId: UUID,
)

interface CoBrowsingUseCase {
    fun requestSession(command: RequestCoBrowsingCommand): Mono<CoBrowsingSession>

    fun startSession(command: StartCoBrowsingCommand): Mono<CoBrowsingSession>

    fun endSession(command: EndCoBrowsingCommand): Mono<CoBrowsingSession>
}

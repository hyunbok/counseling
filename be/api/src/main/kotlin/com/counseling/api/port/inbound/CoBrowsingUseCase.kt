package com.counseling.api.port.inbound

import com.counseling.api.domain.CoBrowsingSession
import reactor.core.publisher.Mono

data class RequestCoBrowsingCommand(
    val channelId: String,
    val agentId: String,
)

data class StartCoBrowsingCommand(
    val channelId: String,
    val sessionId: String,
)

data class EndCoBrowsingCommand(
    val channelId: String,
    val sessionId: String,
)

interface CoBrowsingUseCase {
    fun requestSession(command: RequestCoBrowsingCommand): Mono<CoBrowsingSession>

    fun startSession(command: StartCoBrowsingCommand): Mono<CoBrowsingSession>

    fun endSession(command: EndCoBrowsingCommand): Mono<CoBrowsingSession>
}

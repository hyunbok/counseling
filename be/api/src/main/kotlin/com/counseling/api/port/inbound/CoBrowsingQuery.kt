package com.counseling.api.port.inbound

import com.counseling.api.domain.CoBrowsingSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

data class CoBrowsingSessionPage(
    val sessions: List<CoBrowsingSession>,
    val hasMore: Boolean,
    val oldestTimestamp: Instant?,
)

interface CoBrowsingQuery {
    fun getActiveSession(channelId: UUID): Mono<CoBrowsingSession>

    fun listSessions(
        channelId: UUID,
        before: Instant?,
        limit: Int,
    ): Mono<CoBrowsingSessionPage>

    fun streamUpdates(channelId: UUID): Flux<CoBrowsingSession>
}

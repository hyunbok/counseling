package com.counseling.api.port.inbound

import com.counseling.api.domain.CoBrowsingSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

data class CoBrowsingSessionPage(
    val sessions: List<CoBrowsingSession>,
    val hasMore: Boolean,
    val oldestTimestamp: Instant?,
)

interface CoBrowsingQuery {
    fun getActiveSession(channelId: String): Mono<CoBrowsingSession>

    fun listSessions(
        channelId: String,
        before: Instant?,
        limit: Int,
    ): Mono<CoBrowsingSessionPage>

    fun streamUpdates(channelId: String): Flux<CoBrowsingSession>
}

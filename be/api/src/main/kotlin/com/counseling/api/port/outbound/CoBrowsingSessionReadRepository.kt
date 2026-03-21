package com.counseling.api.port.outbound

import com.counseling.api.domain.CoBrowsingSession
import com.counseling.api.domain.CoBrowsingStatus
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

interface CoBrowsingSessionReadRepository {
    fun save(session: CoBrowsingSession): Mono<CoBrowsingSession>

    fun findByChannelId(
        channelId: String,
        before: Instant?,
        limit: Int,
    ): Flux<CoBrowsingSession>

    fun updateStatus(
        id: String,
        status: CoBrowsingStatus,
        startedAt: Instant?,
        endedAt: Instant?,
        updatedAt: Instant,
    ): Mono<Void>
}

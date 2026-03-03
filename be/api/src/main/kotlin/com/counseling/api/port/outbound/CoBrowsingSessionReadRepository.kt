package com.counseling.api.port.outbound

import com.counseling.api.domain.CoBrowsingSession
import com.counseling.api.domain.CoBrowsingStatus
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

interface CoBrowsingSessionReadRepository {
    fun save(session: CoBrowsingSession): Mono<CoBrowsingSession>

    fun findByChannelId(
        channelId: UUID,
        before: Instant?,
        limit: Int,
    ): Flux<CoBrowsingSession>

    fun updateStatus(
        id: UUID,
        status: CoBrowsingStatus,
        startedAt: Instant?,
        endedAt: Instant?,
        updatedAt: Instant,
    ): Mono<Void>
}

package com.counseling.api.port.outbound

import com.counseling.api.domain.CoBrowsingSession
import reactor.core.publisher.Mono
import java.util.UUID

interface CoBrowsingSessionRepository {
    fun save(session: CoBrowsingSession): Mono<CoBrowsingSession>

    fun findByIdAndNotDeleted(id: UUID): Mono<CoBrowsingSession>

    fun findActiveByChannelId(channelId: UUID): Mono<CoBrowsingSession>
}

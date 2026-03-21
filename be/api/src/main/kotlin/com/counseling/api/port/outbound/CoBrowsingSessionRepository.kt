package com.counseling.api.port.outbound

import com.counseling.api.domain.CoBrowsingSession
import reactor.core.publisher.Mono

interface CoBrowsingSessionRepository {
    fun save(session: CoBrowsingSession): Mono<CoBrowsingSession>

    fun findByIdAndNotDeleted(id: String): Mono<CoBrowsingSession>

    fun findActiveByChannelId(channelId: String): Mono<CoBrowsingSession>
}

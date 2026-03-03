package com.counseling.api.application

import com.counseling.api.domain.CoBrowsingSession
import com.counseling.api.port.inbound.CoBrowsingQuery
import com.counseling.api.port.inbound.CoBrowsingSessionPage
import com.counseling.api.port.outbound.CoBrowsingNotificationPort
import com.counseling.api.port.outbound.CoBrowsingSessionReadRepository
import com.counseling.api.port.outbound.CoBrowsingSessionRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Service
@Profile("!test")
class CoBrowsingQueryService(
    private val coBrowsingSessionRepository: CoBrowsingSessionRepository,
    private val coBrowsingSessionReadRepository: CoBrowsingSessionReadRepository,
    private val coBrowsingNotificationPort: CoBrowsingNotificationPort,
) : CoBrowsingQuery {
    override fun getActiveSession(channelId: UUID): Mono<CoBrowsingSession> =
        coBrowsingSessionRepository.findActiveByChannelId(channelId)

    override fun listSessions(
        channelId: UUID,
        before: Instant?,
        limit: Int,
    ): Mono<CoBrowsingSessionPage> =
        coBrowsingSessionReadRepository
            .findByChannelId(channelId, before, limit + 1)
            .collectList()
            .map { sessions ->
                val hasMore = sessions.size > limit
                val trimmed = if (hasMore) sessions.dropLast(1) else sessions
                val reversed = trimmed.reversed()
                CoBrowsingSessionPage(
                    sessions = reversed,
                    hasMore = hasMore,
                    oldestTimestamp = reversed.firstOrNull()?.createdAt,
                )
            }

    override fun streamUpdates(channelId: UUID): Flux<CoBrowsingSession> =
        coBrowsingNotificationPort.subscribeSessionUpdates(channelId)
}

package com.counseling.api.port.inbound

import com.counseling.api.domain.Feedback
import com.counseling.api.domain.FeedbackStatsResult
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

interface FeedbackQuery {
    fun getByChannelId(channelId: UUID): Mono<Feedback>

    fun getStats(
        from: Instant,
        to: Instant,
    ): Mono<FeedbackStatsResult>
}

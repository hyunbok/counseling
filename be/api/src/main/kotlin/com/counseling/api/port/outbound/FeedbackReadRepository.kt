package com.counseling.api.port.outbound

import com.counseling.api.domain.Feedback
import com.counseling.api.domain.FeedbackStatsResult
import reactor.core.publisher.Mono
import java.time.Instant

interface FeedbackReadRepository {
    fun save(
        feedback: Feedback,
        tenantId: String,
    ): Mono<Feedback>

    fun findByChannelId(
        channelId: String,
        tenantId: String,
    ): Mono<Feedback>

    fun getStats(
        tenantId: String,
        from: Instant,
        to: Instant,
    ): Mono<FeedbackStatsResult>
}

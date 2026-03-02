package com.counseling.api.application

import com.counseling.api.domain.Feedback
import com.counseling.api.domain.FeedbackStatsResult
import com.counseling.api.domain.TenantContext
import com.counseling.api.port.inbound.FeedbackQuery
import com.counseling.api.port.outbound.FeedbackReadRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Service
@Profile("!test")
class FeedbackQueryService(
    private val feedbackReadRepository: FeedbackReadRepository,
) : FeedbackQuery {
    override fun getByChannelId(channelId: UUID): Mono<Feedback> =
        TenantContext.getTenantId().flatMap { tenantId ->
            feedbackReadRepository.findByChannelId(channelId, tenantId)
        }

    override fun getStats(
        from: Instant,
        to: Instant,
    ): Mono<FeedbackStatsResult> =
        TenantContext.getTenantId().flatMap { tenantId ->
            feedbackReadRepository.getStats(tenantId, from, to)
        }
}

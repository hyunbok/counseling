package com.counseling.admin.port.inbound

import com.counseling.admin.domain.Feedback
import reactor.core.publisher.Mono
import java.util.UUID

interface FeedbackQuery {
    fun listFeedbacks(
        agentId: UUID?,
        rating: Int?,
        page: Int,
        size: Int,
    ): Mono<PagedResult<Feedback>>

    fun getFeedback(id: UUID): Mono<Feedback>
}

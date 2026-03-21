package com.counseling.admin.port.inbound

import com.counseling.admin.domain.Feedback
import reactor.core.publisher.Mono

interface FeedbackQuery {
    fun listFeedbacks(
        agentId: String?,
        rating: Int?,
        page: Int,
        size: Int,
    ): Mono<PagedResult<Feedback>>

    fun getFeedback(id: String): Mono<Feedback>
}

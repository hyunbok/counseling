package com.counseling.admin.port.inbound

import com.counseling.admin.domain.Feedback
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface FeedbackQuery {
    fun listFeedbacks(
        agentId: UUID?,
        rating: Int?,
    ): Flux<Feedback>

    fun getFeedback(id: UUID): Mono<Feedback>
}

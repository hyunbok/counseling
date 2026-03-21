package com.counseling.api.port.outbound

import com.counseling.api.domain.Feedback
import reactor.core.publisher.Mono

interface FeedbackRepository {
    fun save(feedback: Feedback): Mono<Feedback>

    fun findByChannelId(channelId: String): Mono<Feedback>
}

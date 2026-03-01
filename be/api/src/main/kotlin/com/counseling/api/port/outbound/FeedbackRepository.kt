package com.counseling.api.port.outbound

import com.counseling.api.domain.Feedback
import reactor.core.publisher.Mono
import java.util.UUID

interface FeedbackRepository {
    fun save(feedback: Feedback): Mono<Feedback>

    fun findByChannelId(channelId: UUID): Mono<Feedback>
}

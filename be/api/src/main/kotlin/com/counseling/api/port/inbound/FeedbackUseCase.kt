package com.counseling.api.port.inbound

import com.counseling.api.domain.Feedback
import reactor.core.publisher.Mono

data class SubmitFeedbackCommand(
    val channelId: String,
    val rating: Int,
    val comment: String?,
)

interface FeedbackUseCase {
    fun submit(command: SubmitFeedbackCommand): Mono<Feedback>
}

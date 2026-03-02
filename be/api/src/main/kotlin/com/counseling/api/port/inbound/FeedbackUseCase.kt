package com.counseling.api.port.inbound

import com.counseling.api.domain.Feedback
import reactor.core.publisher.Mono
import java.util.UUID

data class SubmitFeedbackCommand(
    val channelId: UUID,
    val rating: Int,
    val comment: String?,
)

interface FeedbackUseCase {
    fun submit(command: SubmitFeedbackCommand): Mono<Feedback>
}

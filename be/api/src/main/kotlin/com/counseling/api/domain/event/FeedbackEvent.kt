package com.counseling.api.domain.event

import com.counseling.api.domain.Feedback
import java.time.Instant

sealed class FeedbackEvent : DomainEvent {
    data class Submitted(
        val feedback: Feedback,
        override val occurredAt: Instant = Instant.now(),
    ) : FeedbackEvent()
}

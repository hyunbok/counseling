package com.counseling.api.domain.event

import com.counseling.api.domain.Feedback
import java.time.Instant

data class Submitted(
    val feedback: Feedback,
    override val occurredAt: Instant = Instant.now(),
) : DomainEvent

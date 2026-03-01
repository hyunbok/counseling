package com.counseling.api.domain.event

import java.time.Instant

sealed interface DomainEvent {
    val occurredAt: Instant
}

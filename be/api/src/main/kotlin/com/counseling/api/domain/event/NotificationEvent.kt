package com.counseling.api.domain.event

import com.counseling.api.domain.Notification
import java.time.Instant
import java.util.UUID

sealed class NotificationEvent : DomainEvent {
    data class Created(
        val notification: Notification,
        override val occurredAt: Instant = Instant.now(),
    ) : NotificationEvent()

    data class Read(
        val notificationId: UUID,
        val recipientId: UUID,
        override val occurredAt: Instant = Instant.now(),
    ) : NotificationEvent()

    data class AllRead(
        val recipientId: UUID,
        override val occurredAt: Instant = Instant.now(),
    ) : NotificationEvent()
}

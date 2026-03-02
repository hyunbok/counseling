package com.counseling.api.port.outbound

import com.counseling.api.domain.Notification
import reactor.core.publisher.Flux
import java.util.UUID

interface NotificationSsePort {
    fun emit(
        recipientId: UUID,
        notification: Notification,
    )

    fun subscribe(recipientId: UUID): Flux<Notification>

    fun removeRecipient(recipientId: UUID)
}

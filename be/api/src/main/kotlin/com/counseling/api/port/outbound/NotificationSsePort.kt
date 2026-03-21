package com.counseling.api.port.outbound

import com.counseling.api.domain.Notification
import reactor.core.publisher.Flux

interface NotificationSsePort {
    fun emit(
        recipientId: String,
        notification: Notification,
    )

    fun subscribe(recipientId: String): Flux<Notification>

    fun removeRecipient(recipientId: String)
}

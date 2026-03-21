package com.counseling.api.port.outbound

import com.counseling.api.domain.Notification
import reactor.core.publisher.Mono

interface NotificationRepository {
    fun save(notification: Notification): Mono<Notification>

    fun findByIdAndRecipientId(
        id: String,
        recipientId: String,
    ): Mono<Notification>

    fun markAsRead(
        id: String,
        recipientId: String,
    ): Mono<Boolean>

    fun markAllAsReadByRecipientId(recipientId: String): Mono<Long>
}

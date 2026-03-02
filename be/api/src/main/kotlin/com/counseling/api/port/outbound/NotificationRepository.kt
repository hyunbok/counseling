package com.counseling.api.port.outbound

import com.counseling.api.domain.Notification
import reactor.core.publisher.Mono
import java.util.UUID

interface NotificationRepository {
    fun save(notification: Notification): Mono<Notification>

    fun findByIdAndRecipientId(
        id: UUID,
        recipientId: UUID,
    ): Mono<Notification>

    fun markAsRead(
        id: UUID,
        recipientId: UUID,
    ): Mono<Boolean>

    fun markAllAsReadByRecipientId(recipientId: UUID): Mono<Long>
}

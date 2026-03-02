package com.counseling.api.port.inbound

import com.counseling.api.domain.Notification
import com.counseling.api.domain.NotificationType
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

data class NotificationHistoryResult(
    val notifications: List<Notification>,
    val hasMore: Boolean,
)

interface NotificationQuery {
    fun getHistory(
        recipientId: UUID,
        type: NotificationType?,
        read: Boolean?,
        before: Instant?,
        limit: Int,
    ): Mono<NotificationHistoryResult>

    fun getUnreadCount(recipientId: UUID): Mono<Long>

    fun streamNotifications(recipientId: UUID): Flux<Notification>
}

package com.counseling.api.port.outbound

import com.counseling.api.domain.Notification
import com.counseling.api.domain.NotificationType
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

interface NotificationReadRepository {
    fun save(
        notification: Notification,
        tenantId: String,
    ): Mono<Notification>

    fun findByRecipientId(
        recipientId: String,
        tenantId: String,
        type: NotificationType?,
        read: Boolean?,
        before: Instant?,
        limit: Int,
    ): Flux<Notification>

    fun countUnread(
        recipientId: String,
        tenantId: String,
    ): Mono<Long>

    fun markAsRead(
        notificationId: String,
        tenantId: String,
    ): Mono<Boolean>

    fun markAllAsRead(
        recipientId: String,
        tenantId: String,
    ): Mono<Long>
}

package com.counseling.api.application

import com.counseling.api.domain.Notification
import com.counseling.api.domain.NotificationType
import com.counseling.api.domain.TenantContext
import com.counseling.api.port.inbound.NotificationHistoryResult
import com.counseling.api.port.inbound.NotificationQuery
import com.counseling.api.port.outbound.NotificationReadRepository
import com.counseling.api.port.outbound.NotificationSsePort
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Service
@Profile("!test")
class NotificationQueryService(
    private val notificationReadRepository: NotificationReadRepository,
    private val notificationSsePort: NotificationSsePort,
) : NotificationQuery {
    override fun getHistory(
        recipientId: UUID,
        type: NotificationType?,
        read: Boolean?,
        before: Instant?,
        limit: Int,
    ): Mono<NotificationHistoryResult> =
        TenantContext.getTenantId().flatMap { tenantId ->
            notificationReadRepository
                .findByRecipientId(recipientId, tenantId, type, read, before, limit + 1)
                .collectList()
                .map { notifications ->
                    val hasMore = notifications.size > limit
                    val trimmed = if (hasMore) notifications.dropLast(1) else notifications
                    NotificationHistoryResult(
                        notifications = trimmed,
                        hasMore = hasMore,
                    )
                }
        }

    override fun getUnreadCount(recipientId: UUID): Mono<Long> =
        TenantContext.getTenantId().flatMap { tenantId ->
            notificationReadRepository.countUnread(recipientId, tenantId)
        }

    override fun streamNotifications(recipientId: UUID): Flux<Notification> =
        notificationSsePort
            .subscribe(recipientId)
            .doFinally { notificationSsePort.removeRecipient(recipientId) }
}

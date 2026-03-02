package com.counseling.api.adapter.inbound.web.controller

import com.counseling.api.adapter.inbound.web.dto.NotificationListResponse
import com.counseling.api.adapter.inbound.web.dto.NotificationResponse
import com.counseling.api.adapter.inbound.web.dto.NotificationSseEvent
import com.counseling.api.adapter.inbound.web.dto.UnreadCountResponse
import com.counseling.api.domain.Notification
import com.counseling.api.domain.NotificationType
import com.counseling.api.domain.auth.AuthenticatedAgent
import com.counseling.api.domain.exception.UnauthorizedException
import com.counseling.api.port.inbound.NotificationQuery
import com.counseling.api.port.inbound.NotificationUseCase
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/notifications")
@Profile("!test")
class NotificationController(
    private val notificationUseCase: NotificationUseCase,
    private val notificationQuery: NotificationQuery,
) {
    @GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamNotifications(): Flux<NotificationSseEvent> =
        authenticatedAgent().flatMapMany { agent ->
            notificationQuery.streamNotifications(agent.agentId).map { it.toSseEvent() }
        }

    @GetMapping
    fun getHistory(
        @RequestParam(required = false) type: NotificationType?,
        @RequestParam(required = false) read: Boolean?,
        @RequestParam(required = false) before: Instant?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): Mono<NotificationListResponse> =
        authenticatedAgent().flatMap { agent ->
            notificationQuery
                .getHistory(agent.agentId, type, read, before, limit.coerceIn(1, 100))
                .map { result ->
                    NotificationListResponse(
                        notifications = result.notifications.map { it.toResponse() },
                        hasMore = result.hasMore,
                    )
                }
        }

    @GetMapping("/unread-count")
    fun getUnreadCount(): Mono<UnreadCountResponse> =
        authenticatedAgent().flatMap { agent ->
            notificationQuery.getUnreadCount(agent.agentId).map { count ->
                UnreadCountResponse(count = count)
            }
        }

    @PatchMapping("/{notificationId}/read")
    fun markAsRead(
        @PathVariable notificationId: UUID,
    ): Mono<NotificationResponse> =
        authenticatedAgent().flatMap { agent ->
            notificationUseCase.markAsRead(notificationId, agent.agentId).map { it.toResponse() }
        }

    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun markAllAsRead(): Mono<Void> =
        authenticatedAgent().flatMap { agent ->
            notificationUseCase.markAllAsRead(agent.agentId)
        }

    private fun authenticatedAgent(): Mono<AuthenticatedAgent> =
        ReactiveSecurityContextHolder
            .getContext()
            .flatMap { ctx ->
                val principal = ctx.authentication?.principal
                if (principal is AuthenticatedAgent) {
                    Mono.just(principal)
                } else {
                    Mono.error(UnauthorizedException("Not authenticated"))
                }
            }

    private fun Notification.toResponse(): NotificationResponse =
        NotificationResponse(
            id = id,
            type = type.name,
            title = title,
            body = body,
            referenceId = referenceId,
            referenceType = referenceType,
            read = read,
            createdAt = createdAt,
        )

    private fun Notification.toSseEvent(): NotificationSseEvent =
        NotificationSseEvent(
            id = id,
            type = type.name,
            title = title,
            body = body,
            referenceId = referenceId,
            referenceType = referenceType,
            createdAt = createdAt,
        )
}

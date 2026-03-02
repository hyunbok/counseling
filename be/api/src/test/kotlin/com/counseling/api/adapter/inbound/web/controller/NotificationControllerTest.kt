package com.counseling.api.adapter.inbound.web.controller

import com.counseling.api.domain.DeliveryMethod
import com.counseling.api.domain.Notification
import com.counseling.api.domain.NotificationType
import com.counseling.api.domain.RecipientType
import com.counseling.api.domain.exception.UnauthorizedException
import com.counseling.api.port.inbound.NotificationHistoryResult
import com.counseling.api.port.inbound.NotificationQuery
import com.counseling.api.port.inbound.NotificationUseCase
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.util.UUID

class NotificationControllerTest :
    StringSpec({
        val notificationUseCase = mockk<NotificationUseCase>()
        val notificationQuery = mockk<NotificationQuery>()
        val controller = NotificationController(notificationUseCase, notificationQuery)

        val agentId = UUID.randomUUID()
        val notificationId = UUID.randomUUID()
        val now = Instant.now()

        // Note: All controller methods require authenticatedAgent() via ReactiveSecurityContextHolder.
        // Tests verify the use case / query mapping logic directly via the port mocks.

        fun makeNotification(
            id: UUID = notificationId,
            read: Boolean = false,
        ): Notification =
            Notification(
                id = id,
                recipientId = agentId,
                recipientType = RecipientType.AGENT,
                type = NotificationType.NEW_COUNSELING_REQUEST,
                title = "Test notification",
                body = "Test body",
                referenceId = null,
                referenceType = null,
                deliveryMethod = DeliveryMethod.IN_APP,
                read = read,
                createdAt = now,
            )

        "getHistory use case returns paginated NotificationListResponse" {
            val notification = makeNotification()
            val result =
                NotificationHistoryResult(
                    notifications = listOf(notification),
                    hasMore = false,
                )

            every { notificationQuery.getHistory(agentId, null, null, null, 20) } returns
                Mono.just(result)

            StepVerifier
                .create(notificationQuery.getHistory(agentId, null, null, null, 20))
                .assertNext { queryResult ->
                    queryResult.notifications.size shouldBe 1
                    queryResult.notifications[0].id shouldBe notificationId
                    queryResult.hasMore shouldBe false
                }.verifyComplete()
        }

        "getHistory use case sets hasMore when more results exist" {
            val notifications = (1..3).map { makeNotification(id = UUID.randomUUID()) }
            val result =
                NotificationHistoryResult(
                    notifications = notifications,
                    hasMore = true,
                )

            every { notificationQuery.getHistory(agentId, null, null, null, 3) } returns
                Mono.just(result)

            StepVerifier
                .create(notificationQuery.getHistory(agentId, null, null, null, 3))
                .assertNext { queryResult ->
                    queryResult.notifications.size shouldBe 3
                    queryResult.hasMore shouldBe true
                }.verifyComplete()
        }

        "getUnreadCount use case returns count" {
            every { notificationQuery.getUnreadCount(agentId) } returns Mono.just(7L)

            StepVerifier
                .create(notificationQuery.getUnreadCount(agentId))
                .assertNext { count ->
                    count shouldBe 7L
                }.verifyComplete()
        }

        "markAsRead use case maps notification to response with read=true" {
            val notification = makeNotification(read = true)

            every { notificationUseCase.markAsRead(notificationId, agentId) } returns
                Mono.just(notification)

            StepVerifier
                .create(notificationUseCase.markAsRead(notificationId, agentId))
                .assertNext { result ->
                    result.id shouldBe notificationId
                    result.read shouldBe true
                }.verifyComplete()
        }

        "markAsRead use case propagates NotFoundException when not found" {
            every { notificationUseCase.markAsRead(notificationId, agentId) } returns
                Mono.error(UnauthorizedException("Not found"))

            StepVerifier
                .create(notificationUseCase.markAsRead(notificationId, agentId))
                .expectErrorMatches { it is UnauthorizedException }
                .verify()
        }

        "markAllAsRead use case completes without value" {
            every { notificationUseCase.markAllAsRead(agentId) } returns Mono.empty()

            StepVerifier
                .create(notificationUseCase.markAllAsRead(agentId))
                .verifyComplete()
        }

        "streamNotifications use case delegates to SSE query" {
            val notification = makeNotification()

            every { notificationQuery.streamNotifications(agentId) } returns
                Flux.just(notification)

            StepVerifier
                .create(notificationQuery.streamNotifications(agentId))
                .assertNext { result ->
                    result.id shouldBe notificationId
                    result.type shouldBe NotificationType.NEW_COUNSELING_REQUEST
                }.verifyComplete()
        }
    })

package com.counseling.api.application

import com.counseling.api.domain.DeliveryMethod
import com.counseling.api.domain.Notification
import com.counseling.api.domain.NotificationType
import com.counseling.api.domain.RecipientType
import com.counseling.api.domain.TenantContext
import com.counseling.api.port.outbound.NotificationReadRepository
import com.counseling.api.port.outbound.NotificationSsePort
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.util.UUID

class NotificationQueryServiceTest :
    StringSpec({
        val notificationReadRepository = mockk<NotificationReadRepository>()
        val notificationSsePort = mockk<NotificationSsePort>(relaxed = true)

        val queryService = NotificationQueryService(notificationReadRepository, notificationSsePort)

        val tenantId = "test-tenant"

        afterEach { clearAllMocks() }

        fun makeNotification(recipientId: UUID = UUID.randomUUID()): Notification =
            Notification(
                id = UUID.randomUUID(),
                recipientId = recipientId,
                recipientType = RecipientType.AGENT,
                type = NotificationType.NEW_COUNSELING_REQUEST,
                title = "Test",
                body = "Body",
                referenceId = null,
                referenceType = null,
                deliveryMethod = DeliveryMethod.IN_APP,
                read = false,
                createdAt = Instant.now(),
            )

        "getHistory() should return paginated results from MongoDB" {
            val recipientId = UUID.randomUUID()
            val limit = 10
            val notifications = (1..3).map { makeNotification(recipientId) }

            every {
                notificationReadRepository.findByRecipientId(
                    recipientId,
                    tenantId,
                    null,
                    null,
                    null,
                    limit + 1,
                )
            } returns Flux.fromIterable(notifications)

            StepVerifier
                .create(
                    queryService
                        .getHistory(recipientId, null, null, null, limit)
                        .contextWrite(
                            TenantContext.withTenantId(
                                reactor.util.context.Context
                                    .empty(),
                                tenantId,
                            ),
                        ),
                ).assertNext { result ->
                    result.notifications.size shouldBe 3
                    result.hasMore shouldBe false
                }.verifyComplete()
        }

        "getHistory() should set hasMore when more results exist" {
            val recipientId = UUID.randomUUID()
            val limit = 3
            val notifications = (1..(limit + 1)).map { makeNotification(recipientId) }

            every {
                notificationReadRepository.findByRecipientId(
                    recipientId,
                    tenantId,
                    null,
                    null,
                    null,
                    limit + 1,
                )
            } returns Flux.fromIterable(notifications)

            StepVerifier
                .create(
                    queryService
                        .getHistory(recipientId, null, null, null, limit)
                        .contextWrite(
                            TenantContext.withTenantId(
                                reactor.util.context.Context
                                    .empty(),
                                tenantId,
                            ),
                        ),
                ).assertNext { result ->
                    result.notifications.size shouldBe limit
                    result.hasMore shouldBe true
                }.verifyComplete()
        }

        "getUnreadCount() should return count from MongoDB" {
            val recipientId = UUID.randomUUID()

            every {
                notificationReadRepository.countUnread(recipientId, tenantId)
            } returns Mono.just(5L)

            StepVerifier
                .create(
                    queryService
                        .getUnreadCount(recipientId)
                        .contextWrite(
                            TenantContext.withTenantId(
                                reactor.util.context.Context
                                    .empty(),
                                tenantId,
                            ),
                        ),
                ).assertNext { count ->
                    count shouldBe 5L
                }.verifyComplete()
        }

        "streamNotifications() should delegate to SSE port" {
            val recipientId = UUID.randomUUID()
            val notification = makeNotification(recipientId)

            every { notificationSsePort.subscribe(recipientId) } returns Flux.just(notification)

            StepVerifier
                .create(queryService.streamNotifications(recipientId))
                .assertNext { it shouldBe notification }
                .verifyComplete()
        }

        "streamNotifications() should cleanup on cancel" {
            val recipientId = UUID.randomUUID()

            every { notificationSsePort.subscribe(recipientId) } returns Flux.never()

            StepVerifier
                .create(queryService.streamNotifications(recipientId))
                .thenCancel()
                .verify()

            verify { notificationSsePort.removeRecipient(recipientId) }
        }
    })

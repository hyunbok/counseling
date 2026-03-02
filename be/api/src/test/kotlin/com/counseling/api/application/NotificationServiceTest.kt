package com.counseling.api.application

import com.counseling.api.domain.DeliveryMethod
import com.counseling.api.domain.Notification
import com.counseling.api.domain.NotificationType
import com.counseling.api.domain.RecipientType
import com.counseling.api.domain.TenantContext
import com.counseling.api.domain.exception.NotFoundException
import com.counseling.api.port.inbound.SendNotificationCommand
import com.counseling.api.port.outbound.AgentRepository
import com.counseling.api.port.outbound.EmailMessage
import com.counseling.api.port.outbound.EmailPort
import com.counseling.api.port.outbound.NotificationReadRepository
import com.counseling.api.port.outbound.NotificationRepository
import com.counseling.api.port.outbound.NotificationSsePort
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.util.UUID

class NotificationServiceTest :
    StringSpec({
        val notificationRepository = mockk<NotificationRepository>()
        val notificationReadRepository = mockk<NotificationReadRepository>()
        val notificationSsePort = mockk<NotificationSsePort>(relaxed = true)
        val emailPort = mockk<EmailPort>()
        val agentRepository = mockk<AgentRepository>()

        val service =
            NotificationService(
                notificationRepository,
                notificationReadRepository,
                notificationSsePort,
                emailPort,
                agentRepository,
            )

        val tenantId = "test-tenant"

        afterEach { clearAllMocks() }

        fun makeNotification(deliveryMethod: DeliveryMethod = DeliveryMethod.IN_APP): Notification {
            val recipientId = UUID.randomUUID()
            return Notification(
                id = UUID.randomUUID(),
                recipientId = recipientId,
                recipientType = RecipientType.AGENT,
                type = NotificationType.NEW_COUNSELING_REQUEST,
                title = "Test notification",
                body = "Test body",
                referenceId = null,
                referenceType = null,
                deliveryMethod = deliveryMethod,
                read = false,
                createdAt = Instant.now(),
            )
        }

        fun makeCommand(
            recipientId: UUID = UUID.randomUUID(),
            deliveryMethod: DeliveryMethod = DeliveryMethod.IN_APP,
        ): SendNotificationCommand =
            SendNotificationCommand(
                recipientId = recipientId,
                recipientType = RecipientType.AGENT,
                type = NotificationType.NEW_COUNSELING_REQUEST,
                title = "Test notification",
                body = "Test body",
                referenceId = null,
                referenceType = null,
                deliveryMethod = deliveryMethod,
            )

        "send() with IN_APP delivery should save to PostgreSQL, emit SSE, and project to MongoDB" {
            val command = makeCommand(deliveryMethod = DeliveryMethod.IN_APP)
            val saved =
                makeNotification(DeliveryMethod.IN_APP).copy(
                    recipientId = command.recipientId,
                    deliveryMethod = DeliveryMethod.IN_APP,
                )

            every { notificationRepository.save(any()) } returns Mono.just(saved)
            every { notificationReadRepository.save(saved, tenantId) } returns Mono.just(saved)

            StepVerifier
                .create(
                    service
                        .send(command)
                        .contextWrite(
                            TenantContext.withTenantId(
                                reactor.util.context.Context
                                    .empty(),
                                tenantId,
                            ),
                        ),
                ).assertNext { result ->
                    result.deliveryMethod shouldBe DeliveryMethod.IN_APP
                    result.read shouldBe false
                }.verifyComplete()

            verify { notificationRepository.save(any()) }
            verify { notificationSsePort.emit(saved.recipientId, saved) }
            verify { notificationReadRepository.save(saved, tenantId) }
        }

        "send() with EMAIL delivery should save to PostgreSQL and send email" {
            val command = makeCommand(deliveryMethod = DeliveryMethod.EMAIL)
            val saved =
                makeNotification(DeliveryMethod.EMAIL).copy(
                    recipientId = command.recipientId,
                    deliveryMethod = DeliveryMethod.EMAIL,
                )
            val agent =
                com.counseling.api.domain.Agent(
                    id = command.recipientId,
                    username = "agent1",
                    passwordHash = "hash",
                    name = "Agent One",
                    role = com.counseling.api.domain.AgentRole.COUNSELOR,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                    email = "agent@test.com",
                )

            every { notificationRepository.save(any()) } returns Mono.just(saved)
            every { notificationReadRepository.save(saved, tenantId) } returns Mono.just(saved)
            every { agentRepository.findByIdAndNotDeleted(saved.recipientId) } returns Mono.just(agent)
            every { emailPort.send(any<EmailMessage>()) } returns Mono.empty()

            StepVerifier
                .create(
                    service
                        .send(command)
                        .contextWrite(
                            TenantContext.withTenantId(
                                reactor.util.context.Context
                                    .empty(),
                                tenantId,
                            ),
                        ),
                ).assertNext { result ->
                    result.deliveryMethod shouldBe DeliveryMethod.EMAIL
                }.verifyComplete()

            verify { notificationRepository.save(any()) }
            verify { notificationReadRepository.save(saved, tenantId) }
        }

        "send() should still succeed when MongoDB projection fails" {
            val command = makeCommand(deliveryMethod = DeliveryMethod.IN_APP)
            val saved =
                makeNotification(DeliveryMethod.IN_APP).copy(
                    recipientId = command.recipientId,
                    deliveryMethod = DeliveryMethod.IN_APP,
                )

            every { notificationRepository.save(any()) } returns Mono.just(saved)
            every {
                notificationReadRepository.save(saved, tenantId)
            } returns Mono.error(RuntimeException("MongoDB unavailable"))

            StepVerifier
                .create(
                    service
                        .send(command)
                        .contextWrite(
                            TenantContext.withTenantId(
                                reactor.util.context.Context
                                    .empty(),
                                tenantId,
                            ),
                        ),
                ).assertNext { result ->
                    result.id shouldBe saved.id
                }.verifyComplete()
        }

        "markAsRead() should update PostgreSQL and MongoDB" {
            val notification = makeNotification()
            val notificationId = notification.id
            val recipientId = notification.recipientId

            every {
                notificationRepository.findByIdAndRecipientId(notificationId, recipientId)
            } returns Mono.just(notification)
            every { notificationRepository.markAsRead(notificationId, recipientId) } returns Mono.just(true)
            every {
                notificationReadRepository.markAsRead(notificationId, tenantId)
            } returns Mono.just(true)

            StepVerifier
                .create(
                    service
                        .markAsRead(notificationId, recipientId)
                        .contextWrite(
                            TenantContext.withTenantId(
                                reactor.util.context.Context
                                    .empty(),
                                tenantId,
                            ),
                        ),
                ).assertNext { result ->
                    result.id shouldBe notificationId
                    result.read shouldBe true
                }.verifyComplete()

            verify { notificationRepository.markAsRead(notificationId, recipientId) }
            verify { notificationReadRepository.markAsRead(notificationId, tenantId) }
        }

        "markAsRead() should return error when notification not found" {
            val notificationId = UUID.randomUUID()
            val recipientId = UUID.randomUUID()

            every {
                notificationRepository.findByIdAndRecipientId(notificationId, recipientId)
            } returns Mono.empty()

            StepVerifier
                .create(
                    service
                        .markAsRead(notificationId, recipientId)
                        .contextWrite(
                            TenantContext.withTenantId(
                                reactor.util.context.Context
                                    .empty(),
                                tenantId,
                            ),
                        ),
                ).expectErrorMatches { it is NotFoundException }
                .verify()
        }

        "markAllAsRead() should update both stores" {
            val recipientId = UUID.randomUUID()

            every { notificationRepository.markAllAsReadByRecipientId(recipientId) } returns Mono.just(3L)
            every {
                notificationReadRepository.markAllAsRead(recipientId, tenantId)
            } returns Mono.just(3L)

            StepVerifier
                .create(
                    service
                        .markAllAsRead(recipientId)
                        .contextWrite(
                            TenantContext.withTenantId(
                                reactor.util.context.Context
                                    .empty(),
                                tenantId,
                            ),
                        ),
                ).verifyComplete()

            verify { notificationRepository.markAllAsReadByRecipientId(recipientId) }
            verify { notificationReadRepository.markAllAsRead(recipientId, tenantId) }
        }
    })

package com.counseling.api.application

import com.counseling.api.domain.Channel
import com.counseling.api.domain.ChannelStatus
import com.counseling.api.domain.DeliveryMethod
import com.counseling.api.domain.Feedback
import com.counseling.api.domain.Notification
import com.counseling.api.domain.NotificationType
import com.counseling.api.domain.RecipientType
import com.counseling.api.domain.TenantContext
import com.counseling.api.domain.exception.BadRequestException
import com.counseling.api.domain.exception.ConflictException
import com.counseling.api.domain.exception.NotFoundException
import com.counseling.api.port.inbound.NotificationUseCase
import com.counseling.api.port.inbound.SubmitFeedbackCommand
import com.counseling.api.port.outbound.ChannelRepository
import com.counseling.api.port.outbound.FeedbackReadRepository
import com.counseling.api.port.outbound.FeedbackRepository
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

class FeedbackServiceTest :
    StringSpec({
        val feedbackRepository = mockk<FeedbackRepository>()
        val feedbackReadRepository = mockk<FeedbackReadRepository>()
        val channelRepository = mockk<ChannelRepository>()
        val notificationUseCase = mockk<NotificationUseCase>(relaxed = true)

        val service =
            FeedbackService(
                feedbackRepository,
                feedbackReadRepository,
                channelRepository,
                notificationUseCase,
            )

        val tenantId = "test-tenant"

        afterEach { clearAllMocks() }

        fun makeChannel(agentId: UUID? = UUID.randomUUID()): Channel =
            Channel(
                id = UUID.randomUUID(),
                agentId = agentId,
                status = ChannelStatus.CLOSED,
                startedAt = Instant.now().minusSeconds(3600),
                endedAt = Instant.now().minusSeconds(60),
                recordingPath = null,
                createdAt = Instant.now().minusSeconds(7200),
                updatedAt = Instant.now().minusSeconds(60),
            )

        fun makeCommand(channelId: UUID = UUID.randomUUID()): SubmitFeedbackCommand =
            SubmitFeedbackCommand(
                channelId = channelId,
                rating = 4,
                comment = "Great service",
            )

        fun makeNotification(): Notification =
            Notification(
                id = UUID.randomUUID(),
                recipientId = UUID.randomUUID(),
                recipientType = RecipientType.AGENT,
                type = NotificationType.NEW_FEEDBACK,
                title = "New Feedback Received",
                body = "Rating: 4/5 - Great service",
                referenceId = null,
                referenceType = "FEEDBACK",
                deliveryMethod = DeliveryMethod.IN_APP,
                read = false,
                createdAt = Instant.now(),
            )

        "submit() should save feedback, project to read store, and send notification" {
            val channel = makeChannel()
            val command = makeCommand(channelId = channel.id)

            every { channelRepository.findByIdAndNotDeleted(channel.id) } returns Mono.just(channel)
            every { feedbackRepository.findByChannelId(command.channelId) } returns Mono.empty()
            every { feedbackRepository.save(any()) } answers {
                Mono.just(firstArg())
            }
            every { feedbackReadRepository.save(any(), tenantId) } answers {
                Mono.just(firstArg())
            }
            every { notificationUseCase.send(any()) } returns Mono.just(makeNotification())

            StepVerifier
                .create(
                    service
                        .submit(command)
                        .contextWrite(
                            TenantContext.withTenantId(
                                reactor.util.context.Context
                                    .empty(),
                                tenantId,
                            ),
                        ),
                ).assertNext { result ->
                    result.channelId shouldBe command.channelId
                    result.rating shouldBe 4
                }.verifyComplete()

            verify { feedbackRepository.save(any()) }
            verify { feedbackReadRepository.save(any(), tenantId) }
        }

        "submit() should succeed when channel has no agent (no notification sent)" {
            val channel = makeChannel(agentId = null)
            val command = makeCommand(channelId = channel.id)

            every { channelRepository.findByIdAndNotDeleted(channel.id) } returns Mono.just(channel)
            every { feedbackRepository.findByChannelId(command.channelId) } returns Mono.empty()
            every { feedbackRepository.save(any()) } answers {
                Mono.just(firstArg())
            }
            every { feedbackReadRepository.save(any(), tenantId) } answers {
                Mono.just(firstArg())
            }

            StepVerifier
                .create(
                    service
                        .submit(command)
                        .contextWrite(
                            TenantContext.withTenantId(
                                reactor.util.context.Context
                                    .empty(),
                                tenantId,
                            ),
                        ),
                ).assertNext { result ->
                    result.channelId shouldBe command.channelId
                    result.rating shouldBe 4
                }.verifyComplete()

            verify { feedbackRepository.save(any()) }
            verify { feedbackReadRepository.save(any(), tenantId) }
            verify(exactly = 0) { notificationUseCase.send(any()) }
        }

        "submit() should return error when channel not found" {
            val command = makeCommand()

            every { channelRepository.findByIdAndNotDeleted(command.channelId) } returns Mono.empty()

            StepVerifier
                .create(
                    service
                        .submit(command)
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

        "submit() should still succeed when MongoDB projection fails" {
            val channel = makeChannel(agentId = null)
            val command = makeCommand(channelId = channel.id)
            val savedFeedback =
                Feedback(
                    id = UUID.randomUUID(),
                    channelId = command.channelId,
                    rating = command.rating,
                    comment = command.comment,
                    createdAt = Instant.now(),
                )

            every { channelRepository.findByIdAndNotDeleted(channel.id) } returns Mono.just(channel)
            every { feedbackRepository.findByChannelId(command.channelId) } returns Mono.empty()
            every { feedbackRepository.save(any()) } returns Mono.just(savedFeedback)
            every {
                feedbackReadRepository.save(any(), tenantId)
            } returns Mono.error(RuntimeException("MongoDB unavailable"))

            StepVerifier
                .create(
                    service
                        .submit(command)
                        .contextWrite(
                            TenantContext.withTenantId(
                                reactor.util.context.Context
                                    .empty(),
                                tenantId,
                            ),
                        ),
                ).assertNext { result ->
                    result.channelId shouldBe command.channelId
                    result.rating shouldBe 4
                }.verifyComplete()
        }

        "submit() should return error when feedback already exists for channel" {
            val channel = makeChannel()
            val command = makeCommand(channelId = channel.id)
            val existingFeedback =
                Feedback(
                    id = UUID.randomUUID(),
                    channelId = command.channelId,
                    rating = 5,
                    comment = "Previous feedback",
                    createdAt = Instant.now(),
                )

            every { channelRepository.findByIdAndNotDeleted(channel.id) } returns Mono.just(channel)
            every { feedbackRepository.findByChannelId(command.channelId) } returns Mono.just(existingFeedback)

            StepVerifier
                .create(
                    service
                        .submit(command)
                        .contextWrite(
                            TenantContext.withTenantId(
                                reactor.util.context.Context
                                    .empty(),
                                tenantId,
                            ),
                        ),
                ).expectErrorMatches { it is ConflictException }
                .verify()

            verify(exactly = 0) { feedbackRepository.save(any()) }
        }

        "submit() should return error when rating is out of range" {
            val command =
                SubmitFeedbackCommand(
                    channelId = UUID.randomUUID(),
                    rating = 0,
                    comment = null,
                )

            StepVerifier
                .create(
                    service
                        .submit(command)
                        .contextWrite(
                            TenantContext.withTenantId(
                                reactor.util.context.Context
                                    .empty(),
                                tenantId,
                            ),
                        ),
                ).expectErrorMatches { it is BadRequestException }
                .verify()
        }

        "submit() should return error when comment exceeds max length" {
            val command =
                SubmitFeedbackCommand(
                    channelId = UUID.randomUUID(),
                    rating = 4,
                    comment = "a".repeat(1001),
                )

            StepVerifier
                .create(
                    service
                        .submit(command)
                        .contextWrite(
                            TenantContext.withTenantId(
                                reactor.util.context.Context
                                    .empty(),
                                tenantId,
                            ),
                        ),
                ).expectErrorMatches { it is BadRequestException }
                .verify()
        }
    })

package com.counseling.api.application

import com.counseling.api.domain.DeliveryMethod
import com.counseling.api.domain.Feedback
import com.counseling.api.domain.NotificationType
import com.counseling.api.domain.RecipientType
import com.counseling.api.domain.TenantContext
import com.counseling.api.domain.exception.BadRequestException
import com.counseling.api.domain.exception.ConflictException
import com.counseling.api.domain.exception.NotFoundException
import com.counseling.api.port.inbound.FeedbackUseCase
import com.counseling.api.port.inbound.NotificationUseCase
import com.counseling.api.port.inbound.SendNotificationCommand
import com.counseling.api.port.inbound.SubmitFeedbackCommand
import com.counseling.api.port.outbound.ChannelRepository
import com.counseling.api.port.outbound.FeedbackProjection
import com.counseling.api.port.outbound.FeedbackReadRepository
import com.counseling.api.port.outbound.FeedbackRepository
import com.counseling.api.port.outbound.HistoryReadRepository
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Service
@Profile("!test")
class FeedbackService(
    private val feedbackRepository: FeedbackRepository,
    private val feedbackReadRepository: FeedbackReadRepository,
    private val channelRepository: ChannelRepository,
    private val notificationUseCase: NotificationUseCase,
    private val historyReadRepository: HistoryReadRepository,
) : FeedbackUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun submit(command: SubmitFeedbackCommand): Mono<Feedback> {
        if (command.rating !in 1..5) {
            return Mono.error(BadRequestException("Rating must be between 1 and 5"))
        }
        if (command.comment != null && command.comment.length > MAX_COMMENT_LENGTH) {
            return Mono.error(BadRequestException("Comment must not exceed $MAX_COMMENT_LENGTH characters"))
        }
        return TenantContext.getTenantId().flatMap { tenantId ->
            channelRepository
                .findByIdAndNotDeleted(command.channelId)
                .switchIfEmpty(Mono.error(NotFoundException("Channel not found: ${command.channelId}")))
                .flatMap { channel ->
                    feedbackRepository
                        .findByChannelId(command.channelId)
                        .flatMap<Feedback> {
                            Mono.error(
                                ConflictException(
                                    "Feedback already submitted for channel: ${command.channelId}",
                                ),
                            )
                        }.switchIfEmpty(
                            Mono.defer {
                                val feedback =
                                    Feedback(
                                        id = UUID.randomUUID(),
                                        channelId = command.channelId,
                                        rating = command.rating,
                                        comment = command.comment,
                                        createdAt = Instant.now(),
                                    )
                                feedbackRepository
                                    .save(feedback)
                                    .flatMap { saved ->
                                        feedbackReadRepository
                                            .save(saved, tenantId)
                                            .thenReturn(saved)
                                            .onErrorResume { e ->
                                                log.error(
                                                    "Failed to project feedback {} to read store",
                                                    saved.id,
                                                    e,
                                                )
                                                Mono.just(saved)
                                            }.flatMap { s ->
                                                historyReadRepository
                                                    .updateFeedback(
                                                        channelId = s.channelId,
                                                        tenantId = tenantId,
                                                        feedback =
                                                            FeedbackProjection(
                                                                rating = s.rating,
                                                                comment = s.comment,
                                                                createdAt = s.createdAt,
                                                            ),
                                                    ).onErrorResume { e ->
                                                        log.error(
                                                            "Failed to update history feedback for channel {}",
                                                            s.channelId,
                                                            e,
                                                        )
                                                        Mono.empty()
                                                    }.thenReturn(s)
                                            }
                                    }.doOnNext { saved ->
                                        channel.agentId?.let { agentId ->
                                            notificationUseCase
                                                .send(
                                                    SendNotificationCommand(
                                                        recipientId = agentId,
                                                        recipientType = RecipientType.AGENT,
                                                        type = NotificationType.NEW_FEEDBACK,
                                                        title = "New Feedback Received",
                                                        body =
                                                            "Rating: ${saved.rating}/5" +
                                                                (saved.comment?.let { " - $it" } ?: ""),
                                                        referenceId = saved.id,
                                                        referenceType = "FEEDBACK",
                                                        deliveryMethod = DeliveryMethod.IN_APP,
                                                    ),
                                                ).doOnError { e ->
                                                    log.error(
                                                        "Failed to send feedback notification",
                                                        e,
                                                    )
                                                }.subscribe()
                                        }
                                    }
                            },
                        )
                }
        }
    }

    companion object {
        private const val MAX_COMMENT_LENGTH = 1000
    }
}

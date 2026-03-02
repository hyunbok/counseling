package com.counseling.api.application

import com.counseling.api.domain.DeliveryMethod
import com.counseling.api.domain.Notification
import com.counseling.api.domain.TenantContext
import com.counseling.api.domain.exception.NotFoundException
import com.counseling.api.port.inbound.NotificationUseCase
import com.counseling.api.port.inbound.SendNotificationCommand
import com.counseling.api.port.outbound.EmailMessage
import com.counseling.api.port.outbound.EmailPort
import com.counseling.api.port.outbound.NotificationReadRepository
import com.counseling.api.port.outbound.NotificationRepository
import com.counseling.api.port.outbound.NotificationSsePort
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Service
@Profile("!test")
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val notificationReadRepository: NotificationReadRepository,
    private val notificationSsePort: NotificationSsePort,
    private val emailPort: EmailPort,
) : NotificationUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun send(command: SendNotificationCommand): Mono<Notification> =
        TenantContext.getTenantId().flatMap { tenantId ->
            val notification =
                Notification(
                    id = UUID.randomUUID(),
                    recipientId = command.recipientId,
                    recipientType = command.recipientType,
                    type = command.type,
                    title = command.title,
                    body = command.body,
                    referenceId = command.referenceId,
                    referenceType = command.referenceType,
                    deliveryMethod = command.deliveryMethod,
                    read = false,
                    createdAt = Instant.now(),
                )
            notificationRepository
                .save(notification)
                .doOnNext { saved ->
                    if (saved.deliveryMethod == DeliveryMethod.IN_APP) {
                        notificationSsePort.emit(saved.recipientId, saved)
                    }
                    if (saved.deliveryMethod == DeliveryMethod.EMAIL) {
                        emailPort
                            .send(
                                EmailMessage(
                                    to = saved.recipientId.toString(),
                                    subject = saved.title,
                                    htmlBody = saved.body,
                                ),
                            ).doOnError { e ->
                                log.error(
                                    "Failed to send email notification {} to recipient {}: {}",
                                    saved.id,
                                    saved.recipientId,
                                    e.message,
                                )
                            }.subscribe()
                    }
                }.flatMap { saved ->
                    if (saved.deliveryMethod == DeliveryMethod.IN_APP) {
                        notificationReadRepository
                            .save(saved, tenantId)
                            .thenReturn(saved)
                            .onErrorResume { e ->
                                log.error(
                                    "Failed to project notification {} to read store: {}",
                                    saved.id,
                                    e.message,
                                )
                                Mono.just(saved)
                            }
                    } else {
                        Mono.just(saved)
                    }
                }
        }

    override fun markAsRead(
        notificationId: UUID,
        recipientId: UUID,
    ): Mono<Notification> =
        TenantContext.getTenantId().flatMap { tenantId ->
            notificationRepository
                .findByIdAndRecipientId(notificationId, recipientId)
                .switchIfEmpty(Mono.error(NotFoundException("Notification not found: $notificationId")))
                .flatMap { notification ->
                    notificationRepository
                        .markAsRead(notificationId)
                        .flatMap {
                            val updated = notification.copy(read = true)
                            notificationReadRepository
                                .markAsRead(notificationId, tenantId)
                                .thenReturn(updated)
                                .onErrorResume { e ->
                                    log.error(
                                        "Failed to mark notification {} as read in read store: {}",
                                        notificationId,
                                        e.message,
                                    )
                                    Mono.just(updated)
                                }
                        }
                }
        }

    override fun markAllAsRead(recipientId: UUID): Mono<Void> =
        TenantContext.getTenantId().flatMap { tenantId ->
            notificationRepository
                .markAllAsReadByRecipientId(recipientId)
                .flatMap {
                    notificationReadRepository
                        .markAllAsRead(recipientId, tenantId)
                        .then()
                        .onErrorResume { e ->
                            log.error(
                                "Failed to mark all notifications as read in read store for recipient {}: {}",
                                recipientId,
                                e.message,
                            )
                            Mono.empty()
                        }
                }
        }
}

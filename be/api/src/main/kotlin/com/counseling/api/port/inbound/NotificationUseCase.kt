package com.counseling.api.port.inbound

import com.counseling.api.domain.DeliveryMethod
import com.counseling.api.domain.Notification
import com.counseling.api.domain.NotificationType
import com.counseling.api.domain.RecipientType
import reactor.core.publisher.Mono

data class SendNotificationCommand(
    val recipientId: String,
    val recipientType: RecipientType,
    val type: NotificationType,
    val title: String,
    val body: String,
    val referenceId: String?,
    val referenceType: String?,
    val deliveryMethod: DeliveryMethod,
)

interface NotificationUseCase {
    fun send(command: SendNotificationCommand): Mono<Notification>

    fun markAsRead(
        notificationId: String,
        recipientId: String,
    ): Mono<Notification>

    fun markAllAsRead(recipientId: String): Mono<Void>
}

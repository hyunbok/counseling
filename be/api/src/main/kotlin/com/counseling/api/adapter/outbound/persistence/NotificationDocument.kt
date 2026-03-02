package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.DeliveryMethod
import com.counseling.api.domain.Notification
import com.counseling.api.domain.NotificationType
import com.counseling.api.domain.RecipientType
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.util.UUID

@Document(collection = "notifications")
@CompoundIndexes(
    CompoundIndex(name = "idx_recipient_created", def = "{'recipientId': 1, 'createdAt': -1}"),
)
data class NotificationDocument(
    @Id
    val id: String,
    val tenantId: String,
    val recipientId: String,
    val recipientType: String,
    val type: String,
    val title: String,
    val body: String,
    val referenceId: String?,
    val referenceType: String?,
    val deliveryMethod: String,
    val read: Boolean,
    val createdAt: Instant,
) {
    fun toDomain(): Notification =
        Notification(
            id = UUID.fromString(id),
            recipientId = UUID.fromString(recipientId),
            recipientType = RecipientType.valueOf(recipientType),
            type = NotificationType.valueOf(type),
            title = title,
            body = body,
            referenceId = referenceId?.let { UUID.fromString(it) },
            referenceType = referenceType,
            deliveryMethod = DeliveryMethod.valueOf(deliveryMethod),
            read = read,
            createdAt = createdAt,
        )

    companion object {
        fun fromDomain(
            notification: Notification,
            tenantId: String,
        ): NotificationDocument =
            NotificationDocument(
                id = notification.id.toString(),
                tenantId = tenantId,
                recipientId = notification.recipientId.toString(),
                recipientType = notification.recipientType.name,
                type = notification.type.name,
                title = notification.title,
                body = notification.body,
                referenceId = notification.referenceId?.toString(),
                referenceType = notification.referenceType,
                deliveryMethod = notification.deliveryMethod.name,
                read = notification.read,
                createdAt = notification.createdAt,
            )
    }
}

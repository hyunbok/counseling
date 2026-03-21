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

@Document(collection = "notifications")
@CompoundIndexes(
    CompoundIndex(
        name = "idx_tenant_recipient_created",
        def = "{'tenantId': 1, 'recipientId': 1, 'createdAt': -1}",
    ),
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
            id = id,
            recipientId = recipientId,
            recipientType = RecipientType.valueOf(recipientType),
            type = NotificationType.valueOf(type),
            title = title,
            body = body,
            referenceId = referenceId,
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
                id = notification.id ?: throw IllegalStateException("Notification id must not be null"),
                tenantId = tenantId,
                recipientId = notification.recipientId,
                recipientType = notification.recipientType.name,
                type = notification.type.name,
                title = notification.title,
                body = notification.body,
                referenceId = notification.referenceId,
                referenceType = notification.referenceType,
                deliveryMethod = notification.deliveryMethod.name,
                read = notification.read,
                createdAt = notification.createdAt,
            )
    }
}

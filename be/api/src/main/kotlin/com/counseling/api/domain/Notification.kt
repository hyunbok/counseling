package com.counseling.api.domain

import java.time.Instant
import java.util.UUID

data class Notification(
    val id: UUID,
    val recipientId: UUID,
    val recipientType: RecipientType,
    val type: NotificationType,
    val title: String,
    val body: String,
    val referenceId: UUID?,
    val referenceType: String?,
    val deliveryMethod: DeliveryMethod,
    val read: Boolean = false,
    val createdAt: Instant,
)

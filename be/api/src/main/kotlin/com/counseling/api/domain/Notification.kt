package com.counseling.api.domain

import java.time.Instant

data class Notification(
    val id: String? = null,
    val recipientId: String,
    val recipientType: RecipientType,
    val type: NotificationType,
    val title: String,
    val body: String,
    val referenceId: String?,
    val referenceType: String?,
    val deliveryMethod: DeliveryMethod,
    val read: Boolean = false,
    val createdAt: Instant,
)

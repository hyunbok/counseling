package com.counseling.api.adapter.inbound.web.dto

import java.time.Instant
import java.util.UUID

data class NotificationResponse(
    val id: UUID,
    val type: String,
    val title: String,
    val body: String,
    val referenceId: UUID?,
    val referenceType: String?,
    val read: Boolean,
    val createdAt: Instant,
)

data class NotificationListResponse(
    val notifications: List<NotificationResponse>,
    val hasMore: Boolean,
)

data class UnreadCountResponse(
    val count: Long,
)

data class NotificationSseEvent(
    val id: UUID,
    val type: String,
    val title: String,
    val body: String,
    val referenceId: UUID?,
    val referenceType: String?,
    val createdAt: Instant,
)

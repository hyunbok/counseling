package com.counseling.api.adapter.inbound.web.dto

import java.time.Instant

data class NotificationResponse(
    val id: String?,
    val type: String,
    val title: String,
    val body: String,
    val referenceId: String?,
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
    val id: String?,
    val type: String,
    val title: String,
    val body: String,
    val referenceId: String?,
    val referenceType: String?,
    val createdAt: Instant,
)

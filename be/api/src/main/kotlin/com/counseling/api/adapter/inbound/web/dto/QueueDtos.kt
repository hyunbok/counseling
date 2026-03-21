package com.counseling.api.adapter.inbound.web.dto

import java.time.Instant

data class EnterQueueRequest(
    val name: String,
    val contact: String,
    val groupId: String?,
)

data class EnterQueueResponse(
    val entryId: String,
    val position: Long,
    val queueSize: Long,
)

data class QueueEntryView(
    val entryId: String,
    val name: String,
    val contact: String,
    val groupId: String?,
    val enteredAt: Instant,
    val waitDurationSeconds: Long,
    val position: Long,
)

data class AcceptResponse(
    val channelId: String,
    val customerName: String,
    val customerContact: String,
    val livekitRoomName: String,
    val livekitUrl: String,
    val agentToken: String,
    val customerToken: String,
)

data class PositionResponse(
    val entryId: String,
    val position: Long,
    val queueSize: Long,
)

data class QueueUpdateEvent(
    val type: String,
    val entryId: String?,
    val customerName: String?,
    val queueSize: Long,
    val timestamp: Instant,
)

data class PositionUpdateEvent(
    val entryId: String,
    val position: Long,
    val queueSize: Long,
    val channelId: String? = null,
    val timestamp: Instant,
)

package com.counseling.api.adapter.inbound.web.dto

import java.time.Instant
import java.util.UUID

data class EnterQueueRequest(
    val name: String,
    val contact: String,
    val groupId: UUID?,
)

data class EnterQueueResponse(
    val entryId: UUID,
    val position: Long,
    val queueSize: Long,
)

data class QueueEntryView(
    val entryId: UUID,
    val name: String,
    val contact: String,
    val groupId: UUID?,
    val enteredAt: Instant,
    val waitDurationSeconds: Long,
    val position: Long,
)

data class AcceptResponse(
    val channelId: UUID,
    val customerName: String,
    val customerContact: String,
    val livekitRoomName: String,
    val livekitUrl: String,
    val agentToken: String,
    val customerToken: String,
)

data class PositionResponse(
    val entryId: UUID,
    val position: Long,
    val queueSize: Long,
)

data class QueueUpdateEvent(
    val type: String,
    val entryId: UUID?,
    val customerName: String?,
    val queueSize: Long,
    val timestamp: Instant,
)

data class PositionUpdateEvent(
    val entryId: UUID,
    val position: Long,
    val queueSize: Long,
    val channelId: UUID? = null,
    val timestamp: Instant,
)
